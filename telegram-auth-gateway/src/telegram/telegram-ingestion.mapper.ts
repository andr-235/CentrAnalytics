import { Api } from "telegram";
import type { Entity } from "telegram/define.js";

import type { BackendInboundIntegrationEvent } from "../backend/backend-ingestion.client.js";
import type { TelegramCurrentSession } from "./telegram-auth.types.js";

interface CollectorIdentity {
  userId: number;
  username: string | null;
}

interface CollectorMessageContext {
  message: Api.Message;
  chat?: Entity;
  sender?: Entity;
}

export class TelegramIngestionMapper {
  public mapNewMessage(
    session: TelegramCurrentSession,
    identity: CollectorIdentity,
    context: CollectorMessageContext
  ): BackendInboundIntegrationEvent {
    const chatId = context.message.chatId?.toString() ?? resolveEntityId(context.chat);
    const chatTitle = resolveChatTitle(context.chat, context.message);
    const conversationType = resolveConversationType(context.chat);
    const text = context.message.message || null;

    return {
      platform: "TELEGRAM",
      eventType: "message_new",
      eventId: buildEventId(context.message),
      rawPayload: JSON.stringify({
        chatId,
        messageId: context.message.id,
        senderId: context.message.senderId?.toString() ?? null,
        text
      }),
      signatureValid: true,
      sourceExternalId: String(identity.userId),
      sourceName: `Telegram user ${session.phoneNumber}`,
      sourceSettingsJson: JSON.stringify({
        mode: "user",
        phoneNumber: session.phoneNumber,
        telegramUserId: identity.userId,
        username: identity.username
      }),
      conversation: {
        externalConversationId: chatId,
        type: conversationType,
        title: chatTitle,
        metadataJson: JSON.stringify({
          chatType: resolveChatType(context.chat)
        })
      },
      author: mapAuthor(context.sender),
      message: {
        externalMessageId: String(context.message.id),
        sentAt: new Date(context.message.date * 1000).toISOString(),
        text,
        normalizedText: text ? text.toLowerCase() : null,
        messageType: mapMessageType(context.message),
        replyToExternalMessageId: context.message.replyToMsgId
          ? String(context.message.replyToMsgId)
          : null,
        forwardedFrom: null,
        attachments: []
      }
    };
  }
}

function buildEventId(message: Api.Message): string {
  const editSuffix = message.editDate ? `-${message.editDate}` : "";
  return `telegram-user-message_new-${message.chatId?.toString() ?? "unknown"}-${message.id}${editSuffix}`;
}

function mapAuthor(sender?: Entity) {
  if (!sender) {
    return null;
  }

  if (sender instanceof Api.User) {
    const username = resolveUsername(sender);
    const displayName = buildUserDisplayName(sender);
    return {
      externalUserId: sender.id.toString(),
      displayName,
      username,
      firstName: sender.firstName ?? null,
      lastName: sender.lastName ?? null,
      phone: sender.phone ?? null,
      profileUrl: username ? `https://t.me/${username}` : null,
      bot: sender.bot ?? false,
      metadataJson: JSON.stringify({
        source: "user"
      })
    };
  }

  if (sender instanceof Api.Channel || sender instanceof Api.Chat) {
    return {
      externalUserId: `chat:${sender.id.toString()}`,
      displayName: sender.title ?? null,
      username: sender instanceof Api.Channel ? sender.username ?? null : null,
      firstName: sender.title ?? null,
      lastName: null,
      phone: null,
      profileUrl:
        sender instanceof Api.Channel && sender.username
          ? `https://t.me/${sender.username}`
          : null,
      bot: false,
      metadataJson: JSON.stringify({
        source: "chat"
      })
    };
  }

  return null;
}

function resolveConversationType(chat?: Entity): "DIRECT" | "GROUP" | "CHANNEL" {
  if (chat instanceof Api.User) {
    return "DIRECT";
  }
  if (chat instanceof Api.Channel) {
    return chat.broadcast ? "CHANNEL" : "GROUP";
  }
  return "GROUP";
}

function resolveChatType(chat?: Entity): string {
  if (!chat) {
    return "Unknown";
  }
  return chat.className;
}

function resolveChatTitle(chat: Entity | undefined, message: Api.Message): string | null {
  if (chat instanceof Api.User) {
    return buildUserDisplayName(chat);
  }
  if (chat instanceof Api.Channel || chat instanceof Api.Chat) {
    return chat.title ?? null;
  }
  return message.chatId?.toString() ?? null;
}

function resolveEntityId(entity?: Entity): string {
  if (entity && "id" in entity && entity.id != null) {
    return entity.id.toString();
  }
  return "unknown";
}

function buildUserDisplayName(user: Api.User): string | null {
  const displayName = [user.firstName ?? "", user.lastName ?? ""].join(" ").trim();
  return displayName || resolveUsername(user);
}

function resolveUsername(user: Api.User): string | null {
  if (user.username) {
    return user.username;
  }
  const activeUsername = user.usernames?.find((username) => username.active)?.username;
  if (activeUsername) {
    return activeUsername;
  }
  return null;
}

function mapMessageType(message: Api.Message): "TEXT" | "PHOTO" | "DOCUMENT" | "UNKNOWN" {
  if (!message.media) {
    return "TEXT";
  }
  if (message.media instanceof Api.MessageMediaPhoto) {
    return "PHOTO";
  }
  if (message.media instanceof Api.MessageMediaDocument) {
    return "DOCUMENT";
  }
  return "UNKNOWN";
}
