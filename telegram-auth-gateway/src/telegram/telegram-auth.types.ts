export interface HealthResponse {
  status: "ok";
}

export type TelegramCodeDeliveryType = "app" | "sms" | "unknown";

export interface TelegramAuthTransaction {
  id: string;
  phoneNumber: string;
  phoneCodeHash: string;
  createdAt: string;
  session: string | null;
}

export interface TelegramCurrentSession {
  phoneNumber: string;
  session: string;
  userId: number;
  username: string | null;
  createdAt: string;
}

export interface StartSessionResponse {
  transactionId: string;
  nextType: TelegramCodeDeliveryType;
  codeLength: number | null;
  timeoutSec: number | null;
}

export interface ConfirmSessionResponse {
  session: string;
  userId: number;
  username: string | null;
  phoneNumber: string;
}

export type TelegramAuthErrorCode =
  | "PHONE_NUMBER_REQUIRED"
  | "TRANSACTION_NOT_FOUND_OR_EXPIRED"
  | "PASSWORD_REQUIRED"
  | "TELEGRAM_SEND_CODE_FAILED"
  | "TELEGRAM_INVALID_CODE"
  | "TELEGRAM_PASSWORD_INVALID"
  | "TELEGRAM_SESSION_RESET_FAILED";

export class TelegramAuthError extends Error {
  public readonly code: TelegramAuthErrorCode;

  public constructor(code: TelegramAuthErrorCode, message?: string) {
    super(message ?? code);
    this.code = code;
  }
}
