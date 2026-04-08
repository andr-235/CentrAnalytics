import { describe, expect, it, vi } from "vitest";

import {
  BackendIngestionClient,
  type BackendInboundIntegrationEvent
} from "./backend-ingestion.client.js";

describe("BackendIngestionClient", () => {
  it("posts telegram event to backend internal ingestion endpoint", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true
    });
    const client = new BackendIngestionClient(
      "http://app:8080",
      "internal-token",
      fetchMock as unknown as typeof fetch
    );

    await client.ingestTelegramEvent(validEvent());

    expect(fetchMock).toHaveBeenCalledWith(
      "http://app:8080/api/internal/integrations/telegram-user/events",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({
          "X-Internal-Token": "internal-token"
        })
      })
    );
  });

  it("throws when backend rejects the event", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      text: vi.fn().mockResolvedValue("Invalid internal token")
    });
    const client = new BackendIngestionClient(
      "http://app:8080",
      "internal-token",
      fetchMock as unknown as typeof fetch
    );

    await expect(client.ingestTelegramEvent(validEvent())).rejects.toThrow("Invalid internal token");
  });
});

function validEvent(): BackendInboundIntegrationEvent {
  return {
    platform: "TELEGRAM",
    eventType: "message_new",
    eventId: "telegram-user-message_new-100-200",
    rawPayload: "{\"chatId\":\"100\"}",
    signatureValid: true,
    sourceExternalId: "1",
    sourceName: "Telegram user +79990000001",
    sourceSettingsJson: "{\"mode\":\"user\"}",
    conversation: {
      externalConversationId: "100",
      type: "GROUP",
      title: "Regional Group",
      metadataJson: "{}"
    },
    author: {
      externalUserId: "2001",
      displayName: "Ivan Ivanov",
      username: "ivan",
      firstName: "Ivan",
      lastName: "Ivanov",
      phone: null,
      profileUrl: null,
      bot: false,
      metadataJson: "{}"
    },
    message: {
      externalMessageId: "200",
      sentAt: "2026-04-08T00:00:00.000Z",
      text: "Hello from Telegram",
      normalizedText: "hello from telegram",
      messageType: "TEXT",
      replyToExternalMessageId: null,
      forwardedFrom: null,
      attachments: []
    }
  };
}
