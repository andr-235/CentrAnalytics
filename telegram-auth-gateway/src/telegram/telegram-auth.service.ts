import { randomUUID } from "node:crypto";

import type { TelegramSessionRepository } from "./telegram-session.repository.js";
import {
  TelegramAuthError,
  type ConfirmSessionResponse,
  type StartSessionResponse
} from "./telegram-auth.types.js";

export interface TelegramRequestCodeResult {
  phoneCodeHash: string;
  nextType: "app" | "sms" | "unknown";
  codeLength: number | null;
  timeoutSec: number | null;
}

export interface TelegramSignInPasswordRequiredResult {
  status: "PASSWORD_REQUIRED";
}

export interface TelegramSignInAuthorizedResult {
  status: "AUTHORIZED";
  userId: number;
  username: string | null;
  phoneNumber: string;
}

export type TelegramSignInResult =
  | TelegramSignInPasswordRequiredResult
  | TelegramSignInAuthorizedResult;

export interface TelegramSignInPasswordResult {
  userId: number;
  username: string | null;
  phoneNumber: string;
}

export interface TelegramGatewayClient {
  requestCode(phoneNumber: string): Promise<TelegramRequestCodeResult>;
  signInWithCode(phoneNumber: string, code: string, phoneCodeHash: string): Promise<TelegramSignInResult>;
  signInWithPassword(password: string): Promise<TelegramSignInPasswordResult>;
  exportSession(): Promise<string>;
  disconnect(): Promise<void>;
}

export type TelegramClientFactory = (session: string | null) => Promise<TelegramGatewayClient>;

export interface TelegramSessionLifecycleHooks {
  onSessionReady?(session: {
    phoneNumber: string;
    session: string;
    userId: number;
    username: string | null;
    createdAt: string;
  }): Promise<void>;
  onSessionReset?(): Promise<void>;
}

export class TelegramAuthService {
  public constructor(
    private readonly repository: TelegramSessionRepository,
    private readonly clientFactory: TelegramClientFactory,
    private readonly lifecycleHooks: TelegramSessionLifecycleHooks = {}
  ) {}

  public async startSession(phoneNumber: string): Promise<StartSessionResponse> {
    if (!phoneNumber.trim()) {
      throw new TelegramAuthError("PHONE_NUMBER_REQUIRED");
    }

    const client = await this.clientFactory(null);
    try {
      const request = await client.requestCode(phoneNumber);
      const transactionId = randomUUID();
      const session = await client.exportSession();

      await this.repository.saveTransaction({
        id: transactionId,
        phoneNumber,
        phoneCodeHash: request.phoneCodeHash,
        createdAt: new Date().toISOString(),
        session
      });

      return {
        transactionId,
        nextType: request.nextType,
        codeLength: request.codeLength,
        timeoutSec: request.timeoutSec
      };
    } finally {
      await client.disconnect();
    }
  }

  public async confirmSession(
    transactionId: string,
    code: string,
    password?: string
  ): Promise<ConfirmSessionResponse> {
    const transaction = await this.repository.getTransaction(transactionId);
    if (!transaction) {
      throw new TelegramAuthError("TRANSACTION_NOT_FOUND_OR_EXPIRED");
    }

    const client = await this.clientFactory(transaction.session);

    try {
      if (password) {
        const result = await client.signInWithPassword(password);
        return this.completeAuthorization(transactionId, result, client);
      }

      const result = await client.signInWithCode(transaction.phoneNumber, code, transaction.phoneCodeHash);

      if (result.status === "PASSWORD_REQUIRED") {
        const session = await client.exportSession();
        await this.repository.saveTransaction({
          ...transaction,
          session
        });
        throw new TelegramAuthError("PASSWORD_REQUIRED");
      }

      return this.completeAuthorization(transactionId, result, client);
    } finally {
      await client.disconnect();
    }
  }

  public async getCurrentSession(): Promise<ConfirmSessionResponse | null> {
    const current = await this.repository.getCurrentSession();
    if (!current) {
      return null;
    }

    return {
      session: current.session,
      userId: current.userId,
      username: current.username,
      phoneNumber: current.phoneNumber
    };
  }

  public async resetCurrentSession(): Promise<void> {
    await this.repository.clearAll();
    await this.lifecycleHooks.onSessionReset?.();
  }

  private async completeAuthorization(
    transactionId: string,
    result: TelegramSignInAuthorizedResult | TelegramSignInPasswordResult,
    client: TelegramGatewayClient
  ): Promise<ConfirmSessionResponse> {
    const session = await client.exportSession();
    const current = {
      session,
      userId: result.userId,
      username: result.username,
      phoneNumber: result.phoneNumber,
      createdAt: new Date().toISOString()
    };

    await this.repository.saveCurrentSession(current);
    await this.repository.deleteTransaction(transactionId);
    await this.lifecycleHooks.onSessionReady?.(current);

    return {
      session,
      userId: result.userId,
      username: result.username,
      phoneNumber: result.phoneNumber
    };
  }
}
