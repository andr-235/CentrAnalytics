import { mkdtemp, rm } from "node:fs/promises";
import { join } from "node:path";
import { tmpdir } from "node:os";

import { afterEach, beforeEach, describe, expect, it } from "vitest";

import type {
  TelegramAuthTransaction,
  TelegramCurrentSession
} from "./telegram-auth.types.js";
import { FileSessionStore } from "./file-session-store.js";

describe("FileSessionStore", () => {
  let dataDir: string;
  let store: FileSessionStore;

  beforeEach(async () => {
    dataDir = await mkdtemp(join(tmpdir(), "telegram-auth-gateway-"));
    store = new FileSessionStore(dataDir);
  });

  afterEach(async () => {
    await rm(dataDir, { recursive: true, force: true });
  });

  it("saves and returns auth transactions", async () => {
    const transaction: TelegramAuthTransaction = {
      id: "tx-1",
      phoneNumber: "+79990000001",
      phoneCodeHash: "hash-1",
      createdAt: "2026-04-08T00:00:00.000Z",
      session: "pending-session"
    };

    await store.saveTransaction(transaction);

    await expect(store.getTransaction(transaction.id)).resolves.toEqual(transaction);
  });

  it("saves and returns current session", async () => {
    const currentSession: TelegramCurrentSession = {
      phoneNumber: "+79990000001",
      session: "session-string",
      userId: 123456789,
      username: "centr_analytics",
      createdAt: "2026-04-08T00:01:00.000Z"
    };

    await store.saveCurrentSession(currentSession);

    await expect(store.getCurrentSession()).resolves.toEqual(currentSession);
  });

  it("clears current session and transactions", async () => {
    const transaction: TelegramAuthTransaction = {
      id: "tx-2",
      phoneNumber: "+79990000002",
      phoneCodeHash: "hash-2",
      createdAt: "2026-04-08T00:02:00.000Z",
      session: null
    };
    const currentSession: TelegramCurrentSession = {
      phoneNumber: "+79990000002",
      session: "session-two",
      userId: 987654321,
      username: null,
      createdAt: "2026-04-08T00:03:00.000Z"
    };

    await store.saveTransaction(transaction);
    await store.saveCurrentSession(currentSession);

    await store.clearAll();

    await expect(store.getTransaction(transaction.id)).resolves.toBeNull();
    await expect(store.getCurrentSession()).resolves.toBeNull();
  });
});
