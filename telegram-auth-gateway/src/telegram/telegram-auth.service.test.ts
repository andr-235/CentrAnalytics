import { mkdtemp, rm } from "node:fs/promises";
import { join } from "node:path";
import { tmpdir } from "node:os";

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { FileSessionStore } from "./file-session-store.js";
import { TelegramAuthError } from "./telegram-auth.types.js";
import type {
  TelegramClientFactory,
  TelegramGatewayClient,
  TelegramSignInPasswordResult,
  TelegramSignInResult
} from "./telegram-auth.service.js";
import { TelegramAuthService } from "./telegram-auth.service.js";

function createClientMock() {
  const client: TelegramGatewayClient = {
    requestCode: vi.fn(),
    signInWithCode: vi.fn(),
    signInWithPassword: vi.fn(),
    exportSession: vi.fn(),
    disconnect: vi.fn()
  };

  return client;
}

describe("TelegramAuthService", () => {
  let dataDir: string;
  let store: FileSessionStore;
  let clientFactory: TelegramClientFactory;

  beforeEach(async () => {
    dataDir = await mkdtemp(join(tmpdir(), "telegram-auth-service-"));
    store = new FileSessionStore(dataDir);
    clientFactory = vi.fn();
  });

  afterEach(async () => {
    await rm(dataDir, { recursive: true, force: true });
  });

  it("starts a session and stores transaction state", async () => {
    const client = createClientMock();
    vi.mocked(client.requestCode).mockResolvedValue({
      phoneCodeHash: "hash-1",
      nextType: "app",
      codeLength: 5,
      timeoutSec: 60
    });
    vi.mocked(client.exportSession).mockResolvedValue("pending-session");
    vi.mocked(clientFactory).mockResolvedValue(client);

    const service = new TelegramAuthService(store, clientFactory);

    const result = await service.startSession("+79990000001");

    expect(result.nextType).toBe("app");
    expect(result.codeLength).toBe(5);

    const transaction = await store.getTransaction(result.transactionId);
    expect(transaction).toMatchObject({
      phoneNumber: "+79990000001",
      phoneCodeHash: "hash-1",
      session: "pending-session"
    });
  });

  it("confirms a session with code and saves current session", async () => {
    const startClient = createClientMock();
    vi.mocked(startClient.requestCode).mockResolvedValue({
      phoneCodeHash: "hash-2",
      nextType: "sms",
      codeLength: 5,
      timeoutSec: null
    });
    vi.mocked(startClient.exportSession).mockResolvedValue("pending-session");

    const confirmClient = createClientMock();
    const signInResult: TelegramSignInResult = {
      status: "AUTHORIZED",
      userId: 42,
      username: "centr",
      phoneNumber: "+79990000002"
    };
    vi.mocked(confirmClient.signInWithCode).mockResolvedValue(signInResult);
    vi.mocked(confirmClient.exportSession).mockResolvedValue("final-session");

    vi.mocked(clientFactory)
      .mockResolvedValueOnce(startClient)
      .mockResolvedValueOnce(confirmClient);

    const service = new TelegramAuthService(store, clientFactory);
    const started = await service.startSession("+79990000002");

    const result = await service.confirmSession(started.transactionId, "12345");

    expect(result).toEqual({
      session: "final-session",
      userId: 42,
      username: "centr",
      phoneNumber: "+79990000002"
    });
    await expect(store.getCurrentSession()).resolves.toEqual({
      session: "final-session",
      userId: 42,
      username: "centr",
      phoneNumber: "+79990000002",
      createdAt: expect.any(String)
    });
    await expect(store.getTransaction(started.transactionId)).resolves.toBeNull();
  });

  it("requires password when telegram requests 2fa", async () => {
    const startClient = createClientMock();
    vi.mocked(startClient.requestCode).mockResolvedValue({
      phoneCodeHash: "hash-3",
      nextType: "app",
      codeLength: 5,
      timeoutSec: 60
    });
    vi.mocked(startClient.exportSession).mockResolvedValue("pending-session");

    const confirmClient = createClientMock();
    const passwordRequired: TelegramSignInResult = {
      status: "PASSWORD_REQUIRED"
    };
    vi.mocked(confirmClient.signInWithCode).mockResolvedValue(passwordRequired);
    vi.mocked(confirmClient.exportSession).mockResolvedValue("password-session");

    vi.mocked(clientFactory)
      .mockResolvedValueOnce(startClient)
      .mockResolvedValueOnce(confirmClient);

    const service = new TelegramAuthService(store, clientFactory);
    const started = await service.startSession("+79990000003");

    await expect(service.confirmSession(started.transactionId, "12345")).rejects.toMatchObject({
      code: "PASSWORD_REQUIRED"
    });

    await expect(store.getTransaction(started.transactionId)).resolves.toMatchObject({
      session: "password-session"
    });
  });

  it("confirms a session with password after code step", async () => {
    const startClient = createClientMock();
    vi.mocked(startClient.requestCode).mockResolvedValue({
      phoneCodeHash: "hash-4",
      nextType: "app",
      codeLength: 5,
      timeoutSec: 60
    });
    vi.mocked(startClient.exportSession).mockResolvedValue("pending-session");

    const passwordStepClient = createClientMock();
    vi.mocked(passwordStepClient.signInWithCode).mockResolvedValue({
      status: "PASSWORD_REQUIRED"
    });
    vi.mocked(passwordStepClient.exportSession).mockResolvedValue("password-session");

    const finishClient = createClientMock();
    const passwordResult: TelegramSignInPasswordResult = {
      userId: 77,
      username: null,
      phoneNumber: "+79990000004"
    };
    vi.mocked(finishClient.signInWithPassword).mockResolvedValue(passwordResult);
    vi.mocked(finishClient.exportSession).mockResolvedValue("final-password-session");

    vi.mocked(clientFactory)
      .mockResolvedValueOnce(startClient)
      .mockResolvedValueOnce(passwordStepClient)
      .mockResolvedValueOnce(finishClient);

    const service = new TelegramAuthService(store, clientFactory);
    const started = await service.startSession("+79990000004");

    await expect(service.confirmSession(started.transactionId, "12345")).rejects.toBeInstanceOf(
      TelegramAuthError
    );

    const result = await service.confirmSession(started.transactionId, "12345", "secret");

    expect(result).toEqual({
      session: "final-password-session",
      userId: 77,
      username: null,
      phoneNumber: "+79990000004"
    });
    await expect(store.getTransaction(started.transactionId)).resolves.toBeNull();
  });

  it("returns current session and resets state", async () => {
    const client = createClientMock();
    vi.mocked(client.requestCode).mockResolvedValue({
      phoneCodeHash: "hash-5",
      nextType: "sms",
      codeLength: 5,
      timeoutSec: 30
    });
    vi.mocked(client.exportSession).mockResolvedValue("pending-session");

    const confirmClient = createClientMock();
    vi.mocked(confirmClient.signInWithCode).mockResolvedValue({
      status: "AUTHORIZED",
      userId: 88,
      username: "active",
      phoneNumber: "+79990000005"
    });
    vi.mocked(confirmClient.exportSession).mockResolvedValue("active-session");

    vi.mocked(clientFactory)
      .mockResolvedValueOnce(client)
      .mockResolvedValueOnce(confirmClient);

    const service = new TelegramAuthService(store, clientFactory);
    const started = await service.startSession("+79990000005");
    await service.confirmSession(started.transactionId, "12345");

    await expect(service.getCurrentSession()).resolves.toEqual({
      session: "active-session",
      userId: 88,
      username: "active",
      phoneNumber: "+79990000005"
    });

    await service.resetCurrentSession();

    await expect(service.getCurrentSession()).resolves.toBeNull();
  });
});
