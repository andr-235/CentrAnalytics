import { Api, TelegramClient, sessions } from "telegram";
import type { Entity } from "telegram/define.js";
import { NewMessage, type NewMessageEvent } from "telegram/events/index.js";

import type { BackendIngestionClient } from "../backend/backend-ingestion.client.js";
import type { TelegramSessionRepository } from "./telegram-session.repository.js";
import type { TelegramCurrentSession } from "./telegram-auth.types.js";
import { TelegramIngestionMapper } from "./telegram-ingestion.mapper.js";
import type { TelegramCollectorStatus } from "./telegram-collector.types.js";

const { StringSession } = sessions;

export interface TelegramCollectorRuntime {
  start(): Promise<CollectorIdentity>;
  stop(): Promise<void>;
}

export interface CollectorIdentity {
  userId: number;
  username: string | null;
}

export type TelegramCollectorRuntimeFactory = (session: TelegramCurrentSession) => TelegramCollectorRuntime;

export class TelegramCollectorService {
  private status: TelegramCollectorStatus = {
    state: "STOPPED",
    lastEventAt: null,
    lastError: null,
    selfUserId: null,
    selfUsername: null
  };

  private activeRuntime: TelegramCollectorRuntime | null = null;
  private activeSessionKey: string | null = null;

  public constructor(
    private readonly repository: TelegramSessionRepository,
    private readonly runtimeFactory: TelegramCollectorRuntimeFactory
  ) {}

  public async startFromCurrentSession(): Promise<void> {
    const currentSession = await this.repository.getCurrentSession();
    if (!currentSession) {
      await this.stop();
      return;
    }

    const sessionKey = `${currentSession.phoneNumber}:${currentSession.session}`;
    if (this.activeRuntime && this.activeSessionKey === sessionKey && this.status.state === "RUNNING") {
      return;
    }

    await this.stop();
    this.status = {
      ...this.status,
      state: "STARTING",
      lastError: null
    };

    const runtime = this.runtimeFactory(currentSession);
    this.activeRuntime = runtime;
    this.activeSessionKey = sessionKey;

    try {
      const identity = await runtime.start();
      this.status = {
        ...this.status,
        state: "RUNNING",
        selfUserId: identity.userId,
        selfUsername: identity.username,
        lastError: null
      };
    } catch (error) {
      this.status = {
        ...this.status,
        state: "FAILED",
        lastError: error instanceof Error ? error.message : "Collector start failed"
      };
      this.activeRuntime = null;
      this.activeSessionKey = null;
      throw error;
    }
  }

  public async stop(): Promise<void> {
    if (this.activeRuntime) {
      await this.activeRuntime.stop();
    }
    this.activeRuntime = null;
    this.activeSessionKey = null;
    this.status = {
      ...this.status,
      state: "STOPPED"
    };
  }

  public async onSessionReady(): Promise<void> {
    await this.startFromCurrentSession();
  }

  public async onSessionReset(): Promise<void> {
    await this.stop();
  }

  public markEventReceived(): void {
    this.status = {
      ...this.status,
      lastEventAt: new Date().toISOString(),
      lastError: null
    };
  }

  public markRuntimeError(error: unknown): void {
    this.status = {
      ...this.status,
      state: "FAILED",
      lastError: error instanceof Error ? error.message : "Collector runtime failed"
    };
  }

  public getStatus(): TelegramCollectorStatus {
    return this.status;
  }
}

interface GramJsCollectorRuntimeOptions {
  apiId: number;
  apiHash: string;
  currentSession: TelegramCurrentSession;
  backendIngestionClient: BackendIngestionClient;
  collectorService: TelegramCollectorService;
  proxy?: {
    enabled: boolean;
    host: string;
    port: number;
    username?: string;
    password?: string;
  };
}

export function createGramJsCollectorRuntimeFactory(
  options: Omit<GramJsCollectorRuntimeOptions, "currentSession" | "collectorService"> & {
    collectorService: TelegramCollectorService;
  }
): TelegramCollectorRuntimeFactory {
  return (currentSession) =>
    new GramJsTelegramCollectorRuntime({
      ...options,
      currentSession
    });
}

class GramJsTelegramCollectorRuntime implements TelegramCollectorRuntime {
  private readonly mapper = new TelegramIngestionMapper();
  private readonly event = new NewMessage({ incoming: true });
  private readonly client: TelegramClient;
  private readonly handler: (event: NewMessageEvent) => Promise<void>;

  public constructor(private readonly options: GramJsCollectorRuntimeOptions) {
    this.client = new TelegramClient(
      new StringSession(options.currentSession.session),
      options.apiId,
      options.apiHash,
      {
        connectionRetries: 5,
        useWSS: false,
        proxy:
          options.proxy?.enabled === true
            ? {
                ip: options.proxy.host,
                port: options.proxy.port,
                socksType: 5,
                username: options.proxy.username,
                password: options.proxy.password
              }
            : undefined
      }
    );
    this.handler = async (event) => {
      try {
        const chat = (await event.message.getChat()) as Entity | undefined;
        const sender = (await event.message.getSender()) as Entity | undefined;
        const me = await this.client.getMe();
        const payload = this.mapper.mapNewMessage(
          this.options.currentSession,
          {
            userId: Number(me.id),
            username: me.username ?? null
          },
          {
            message: event.message,
            chat,
            sender
          }
        );
        await this.options.backendIngestionClient.ingestTelegramEvent(payload);
        this.options.collectorService.markEventReceived();
      } catch (error) {
        this.options.collectorService.markRuntimeError(error);
      }
    };
  }

  public async start(): Promise<CollectorIdentity> {
    await this.client.connect();
    const me = await this.client.getMe();
    this.client.addEventHandler(this.handler, this.event);
    return {
      userId: Number(me.id),
      username: me.username ?? null
    };
  }

  public async stop(): Promise<void> {
    this.client.removeEventHandler(this.handler, this.event);
    await this.client.disconnect();
  }
}
