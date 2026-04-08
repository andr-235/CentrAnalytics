import { describe, expect, it, vi } from "vitest";

import type { TelegramCurrentSession } from "./telegram-auth.types.js";
import type { TelegramSessionRepository } from "./telegram-session.repository.js";
import { TelegramCollectorService, type TelegramCollectorRuntime } from "./telegram-collector.service.js";

describe("TelegramCollectorService", () => {
  it("starts collector from saved session", async () => {
    const repository = createRepository({
      phoneNumber: "+79990000001",
      session: "saved-session",
      userId: 123,
      username: "centr",
      createdAt: "2026-04-08T00:00:00.000Z"
    });
    const runtime = createRuntime();
    const service = new TelegramCollectorService(repository, () => runtime);

    await service.startFromCurrentSession();

    expect(runtime.start).toHaveBeenCalled();
    expect(service.getStatus().state).toBe("RUNNING");
    expect(service.getStatus().selfUserId).toBe(123);
  });

  it("stops collector on reset hook", async () => {
    const repository = createRepository({
      phoneNumber: "+79990000001",
      session: "saved-session",
      userId: 123,
      username: "centr",
      createdAt: "2026-04-08T00:00:00.000Z"
    });
    const runtime = createRuntime();
    const service = new TelegramCollectorService(repository, () => runtime);

    await service.startFromCurrentSession();
    await service.onSessionReset();

    expect(runtime.stop).toHaveBeenCalled();
    expect(service.getStatus().state).toBe("STOPPED");
  });

  it("marks collector as failed when runtime start throws", async () => {
    const repository = createRepository({
      phoneNumber: "+79990000001",
      session: "saved-session",
      userId: 123,
      username: "centr",
      createdAt: "2026-04-08T00:00:00.000Z"
    });
    const runtime = {
      start: vi.fn().mockRejectedValue(new Error("boom")),
      stop: vi.fn().mockResolvedValue(undefined)
    };
    const service = new TelegramCollectorService(repository, () => runtime);

    await expect(service.startFromCurrentSession()).rejects.toThrow("boom");
    expect(service.getStatus().state).toBe("FAILED");
    expect(service.getStatus().lastError).toBe("boom");
  });
});

function createRepository(currentSession: TelegramCurrentSession): TelegramSessionRepository {
  return {
    saveTransaction: vi.fn(),
    getTransaction: vi.fn(),
    deleteTransaction: vi.fn(),
    saveCurrentSession: vi.fn(),
    getCurrentSession: vi.fn().mockResolvedValue(currentSession),
    clearAll: vi.fn()
  };
}

function createRuntime(): TelegramCollectorRuntime & { start: ReturnType<typeof vi.fn>; stop: ReturnType<typeof vi.fn> } {
  return {
    start: vi.fn().mockResolvedValue({
      userId: 123,
      username: "centr"
    }),
    stop: vi.fn().mockResolvedValue(undefined)
  };
}
