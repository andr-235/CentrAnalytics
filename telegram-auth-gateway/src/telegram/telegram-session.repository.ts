import type {
  TelegramAuthTransaction,
  TelegramCurrentSession
} from "./telegram-auth.types.js";

export interface TelegramSessionRepository {
  saveTransaction(transaction: TelegramAuthTransaction): Promise<void>;
  getTransaction(transactionId: string): Promise<TelegramAuthTransaction | null>;
  deleteTransaction(transactionId: string): Promise<void>;
  saveCurrentSession(session: TelegramCurrentSession): Promise<void>;
  getCurrentSession(): Promise<TelegramCurrentSession | null>;
  clearAll(): Promise<void>;
}
