import { Api } from "telegram";
import bigInt from "big-integer";
import { describe, expect, it } from "vitest";

import type { TelegramCurrentSession } from "./telegram-auth.types.js";
import { TelegramIngestionMapper } from "./telegram-ingestion.mapper.js";

describe("TelegramIngestionMapper", () => {
  it("maps group text messages into backend ingestion payload", () => {
    const mapper = new TelegramIngestionMapper();
    const session: TelegramCurrentSession = {
      phoneNumber: "+79990000001",
      session: "saved-session",
      userId: 700000001,
      username: "centr",
      createdAt: "2026-04-08T00:00:00.000Z"
    };
    const message = new Api.Message({
      id: 200,
      peerId: new Api.PeerChannel({ channelId: bigInt(100) }),
      message: "Hello from Telegram",
      date: 1_759_000_000,
      fromId: new Api.PeerUser({ userId: bigInt(2001) }),
      replyTo: new Api.MessageReplyHeader({
        replyToMsgId: 100
      })
    });
    const chat = new Api.Channel({
      id: bigInt(100),
      title: "Regional Group",
      broadcast: false,
      megagroup: true,
      photo: new Api.ChatPhotoEmpty(),
      date: 1_759_000_000
    });
    const sender = new Api.User({
      id: bigInt(2001),
      firstName: "Ivan",
      lastName: "Ivanov",
      username: "ivan"
    });

    const payload = mapper.mapNewMessage(session, { userId: 700000001, username: "centr" }, {
      message,
      chat,
      sender
    });

    expect(payload.platform).toBe("TELEGRAM");
    expect(payload.conversation.externalConversationId).toBe("-100100");
    expect(payload.conversation.type).toBe("GROUP");
    expect(payload.author?.externalUserId).toBe("2001");
    expect(payload.message.externalMessageId).toBe("200");
    expect(payload.message.replyToExternalMessageId).toBe("100");
  });
});
