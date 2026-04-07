import { describe, expect, it, vi } from "vitest";

import { createApp } from "../app.js";
import type { TelegramAuthService } from "./telegram-auth.service.js";

function createTelegramAuthServiceMock(): TelegramAuthService {
  return {
    startSession: vi.fn(),
    confirmSession: vi.fn(),
    getCurrentSession: vi.fn(),
    resetCurrentSession: vi.fn()
  } as unknown as TelegramAuthService;
}

describe("telegram auth routes", () => {
  it("starts a session", async () => {
    const telegramAuthService = createTelegramAuthServiceMock();
    vi.mocked(telegramAuthService.startSession).mockResolvedValue({
      transactionId: "tx-1",
      nextType: "app",
      codeLength: 5,
      timeoutSec: null
    });

    const app = createApp({ telegramAuthService });

    try {
      const response = await app.inject({
        method: "POST",
        url: "/session/start",
        payload: { phoneNumber: "+79990000001" }
      });

      expect(response.statusCode).toBe(200);
      expect(response.json()).toEqual({
        transactionId: "tx-1",
        nextType: "app",
        codeLength: 5,
        timeoutSec: null
      });
    } finally {
      await app.close();
    }
  });

  it("confirms a session", async () => {
    const telegramAuthService = createTelegramAuthServiceMock();
    vi.mocked(telegramAuthService.confirmSession).mockResolvedValue({
      session: "session",
      userId: 1,
      username: "user",
      phoneNumber: "+79990000001"
    });

    const app = createApp({ telegramAuthService });

    try {
      const response = await app.inject({
        method: "POST",
        url: "/session/confirm",
        payload: { transactionId: "tx-1", code: "12345" }
      });

      expect(response.statusCode).toBe(200);
      expect(response.json()).toEqual({
        session: "session",
        userId: 1,
        username: "user",
        phoneNumber: "+79990000001"
      });
    } finally {
      await app.close();
    }
  });

  it("returns current session", async () => {
    const telegramAuthService = createTelegramAuthServiceMock();
    vi.mocked(telegramAuthService.getCurrentSession).mockResolvedValue({
      session: "session",
      userId: 2,
      username: null,
      phoneNumber: "+79990000002"
    });

    const app = createApp({ telegramAuthService });

    try {
      const response = await app.inject({
        method: "GET",
        url: "/session/current"
      });

      expect(response.statusCode).toBe(200);
      expect(response.json()).toEqual({
        session: "session",
        userId: 2,
        username: null,
        phoneNumber: "+79990000002"
      });
    } finally {
      await app.close();
    }
  });

  it("resets current session", async () => {
    const telegramAuthService = createTelegramAuthServiceMock();
    vi.mocked(telegramAuthService.resetCurrentSession).mockResolvedValue();

    const app = createApp({ telegramAuthService });

    try {
      const response = await app.inject({
        method: "DELETE",
        url: "/session/current"
      });

      expect(response.statusCode).toBe(204);
    } finally {
      await app.close();
    }
  });
});
