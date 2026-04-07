import { mkdir, readFile, rename, rm, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";

import type {
  TelegramAuthTransaction,
  TelegramCurrentSession
} from "./telegram-auth.types.js";
import type { TelegramSessionRepository } from "./telegram-session.repository.js";

interface TransactionsData {
  transactions: TelegramAuthTransaction[];
}

export class FileSessionStore implements TelegramSessionRepository {
  private readonly transactionsPath: string;
  private readonly currentSessionPath: string;

  public constructor(private readonly dataDir: string) {
    this.transactionsPath = join(dataDir, "transactions.json");
    this.currentSessionPath = join(dataDir, "current-session.json");
  }

  public async saveTransaction(transaction: TelegramAuthTransaction): Promise<void> {
    const current = await this.readTransactions();
    const next = current.filter((item) => item.id !== transaction.id);
    next.push(transaction);
    await this.writeJson(this.transactionsPath, { transactions: next });
  }

  public async getTransaction(transactionId: string): Promise<TelegramAuthTransaction | null> {
    const current = await this.readTransactions();
    return current.find((item) => item.id === transactionId) ?? null;
  }

  public async deleteTransaction(transactionId: string): Promise<void> {
    const current = await this.readTransactions();
    const next = current.filter((item) => item.id !== transactionId);
    await this.writeJson(this.transactionsPath, { transactions: next });
  }

  public async saveCurrentSession(session: TelegramCurrentSession): Promise<void> {
    await this.writeJson(this.currentSessionPath, session);
  }

  public async getCurrentSession(): Promise<TelegramCurrentSession | null> {
    return this.readJson<TelegramCurrentSession>(this.currentSessionPath);
  }

  public async clearAll(): Promise<void> {
    await Promise.all([
      rm(this.transactionsPath, { force: true }),
      rm(this.currentSessionPath, { force: true })
    ]);
  }

  private async readTransactions(): Promise<TelegramAuthTransaction[]> {
    const data = await this.readJson<TransactionsData>(this.transactionsPath);
    return data?.transactions ?? [];
  }

  private async readJson<T>(filePath: string): Promise<T | null> {
    try {
      const raw = await readFile(filePath, "utf8");
      return JSON.parse(raw) as T;
    } catch (error) {
      if (this.isNotFoundError(error)) {
        return null;
      }

      throw error;
    }
  }

  private async writeJson(filePath: string, value: unknown): Promise<void> {
    await mkdir(dirname(filePath), { recursive: true });
    const tempFilePath = `${filePath}.tmp`;
    await writeFile(tempFilePath, JSON.stringify(value, null, 2), "utf8");
    await rename(tempFilePath, filePath);
  }

  private isNotFoundError(error: unknown): error is NodeJS.ErrnoException {
    return typeof error === "object" && error !== null && "code" in error && error.code === "ENOENT";
  }
}
