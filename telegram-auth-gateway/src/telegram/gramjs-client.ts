import { Api, TelegramClient, sessions } from "telegram";

import {
  TelegramAuthError,
  type TelegramCodeDeliveryType
} from "./telegram-auth.types.js";
import type {
  TelegramClientFactory,
  TelegramGatewayClient,
  TelegramRequestCodeResult,
  TelegramSignInPasswordResult,
  TelegramSignInResult
} from "./telegram-auth.service.js";

const { StringSession } = sessions;

export interface TelegramClientFactoryOptions {
  apiId: number;
  apiHash: string;
  proxy?: {
    enabled: boolean;
    host: string;
    port: number;
    username?: string;
    password?: string;
  };
}

export function createTelegramClientFactory(
  options: TelegramClientFactoryOptions
): TelegramClientFactory {
  return async (session) => {
    const client = new TelegramClient(new StringSession(session ?? ""), options.apiId, options.apiHash, {
      connectionRetries: 3,
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
    });

    await client.connect();

    const gatewayClient: TelegramGatewayClient = {
      async requestCode(phoneNumber: string): Promise<TelegramRequestCodeResult> {
        try {
          const response = await client.sendCode(
            {
              apiId: options.apiId,
              apiHash: options.apiHash
            },
            phoneNumber,
            false
          );

          return {
            phoneCodeHash: response.phoneCodeHash,
            nextType: response.isCodeViaApp ? "app" : "sms",
            codeLength: 5,
            timeoutSec: null
          };
        } catch (error) {
          throw mapTelegramError(error, "TELEGRAM_SEND_CODE_FAILED");
        }
      },
      async signInWithCode(
        phoneNumber: string,
        code: string,
        phoneCodeHash: string
      ): Promise<TelegramSignInResult> {
        try {
          const user = await client.signInUser(
            {
              apiId: options.apiId,
              apiHash: options.apiHash
            },
            {
              phoneNumber,
              phoneCode: async () => code,
              onError: async (error: Error) => {
                if (error.message.toUpperCase().includes("PASSWORD")) {
                  return false;
                }

                return true;
              }
            }
          );

          return {
            status: "AUTHORIZED",
            userId: Number(user.id),
            username: getUsername(user),
            phoneNumber: getPhoneNumber(user) ?? phoneNumber
          };
        } catch (error) {
          if (isPasswordRequired(error)) {
            return { status: "PASSWORD_REQUIRED" };
          }

          throw mapTelegramError(error, "TELEGRAM_INVALID_CODE");
        }
      },
      async signInWithPassword(password: string): Promise<TelegramSignInPasswordResult> {
        try {
          const user = await client.signInWithPassword(
            {
              apiId: options.apiId,
              apiHash: options.apiHash
            },
            {
              password: async () => password,
              onError: async () => true
            }
          );

          return {
            userId: Number(user.id),
            username: getUsername(user),
            phoneNumber: getPhoneNumber(user) ?? ""
          };
        } catch (error) {
          throw mapTelegramError(error, "TELEGRAM_PASSWORD_INVALID");
        }
      },
      async exportSession(): Promise<string> {
        return client.session.save() as unknown as string;
      },
      async disconnect(): Promise<void> {
        await client.disconnect();
      }
    };

    return gatewayClient;
  };
}

function mapTelegramError(error: unknown, fallbackCode: "TELEGRAM_SEND_CODE_FAILED" | "TELEGRAM_INVALID_CODE" | "TELEGRAM_PASSWORD_INVALID"): TelegramAuthError {
  if (error instanceof TelegramAuthError) {
    return error;
  }

  if (typeof error === "object" && error !== null && "errorMessage" in error) {
    const message = String(error.errorMessage);
    if (message.includes("PHONE_NUMBER")) {
      return new TelegramAuthError("PHONE_NUMBER_REQUIRED", message);
    }
    if (message.includes("PHONE_CODE_INVALID") || message.includes("PHONE_CODE_EXPIRED")) {
      return new TelegramAuthError("TELEGRAM_INVALID_CODE", message);
    }
    if (message.includes("PASSWORD_HASH_INVALID")) {
      return new TelegramAuthError("TELEGRAM_PASSWORD_INVALID", message);
    }

    return new TelegramAuthError(fallbackCode, message);
  }

  if (error instanceof Error) {
    return new TelegramAuthError(fallbackCode, error.message);
  }

  return new TelegramAuthError(fallbackCode);
}

function isPasswordRequired(error: unknown): boolean {
  if (typeof error === "object" && error !== null && "errorMessage" in error) {
    return String(error.errorMessage).toUpperCase().includes("PASSWORD");
  }

  return error instanceof Error && error.message.toUpperCase().includes("PASSWORD");
}

function getUsername(user: Api.TypeUser): string | null {
  return user instanceof Api.User ? user.username ?? null : null;
}

function getPhoneNumber(user: Api.TypeUser): string | null {
  return user instanceof Api.User ? user.phone ?? null : null;
}
