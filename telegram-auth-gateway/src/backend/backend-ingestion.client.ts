export interface BackendInboundConversation {
  externalConversationId: string;
  type: "DIRECT" | "GROUP" | "CHANNEL";
  title: string | null;
  metadataJson: string;
}

export interface BackendInboundAuthor {
  externalUserId: string;
  displayName: string | null;
  username: string | null;
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  profileUrl: string | null;
  bot: boolean;
  metadataJson: string;
}

export interface BackendInboundAttachment {
  attachmentType: string;
  externalAttachmentId: string | null;
  url: string | null;
  mimeType: string | null;
  metadataJson: string;
}

export interface BackendInboundMessage {
  externalMessageId: string;
  sentAt: string;
  text: string | null;
  normalizedText: string | null;
  messageType: "TEXT" | "PHOTO" | "DOCUMENT" | "UNKNOWN";
  replyToExternalMessageId: string | null;
  forwardedFrom: string | null;
  attachments: BackendInboundAttachment[];
}

export interface BackendInboundIntegrationEvent {
  platform: "TELEGRAM";
  eventType: string;
  eventId: string;
  rawPayload: string;
  signatureValid: true;
  sourceExternalId: string;
  sourceName: string;
  sourceSettingsJson: string;
  conversation: BackendInboundConversation;
  author: BackendInboundAuthor | null;
  message: BackendInboundMessage;
}

export class BackendIngestionClient {
  public constructor(
    private readonly baseUrl: string,
    private readonly internalToken: string,
    private readonly fetchImpl: typeof fetch = fetch
  ) {}

  public async ingestTelegramEvent(event: BackendInboundIntegrationEvent): Promise<void> {
    const response = await this.fetchImpl(`${this.baseUrl}/api/internal/integrations/telegram-user/events`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Internal-Token": this.internalToken
      },
      body: JSON.stringify(event)
    });

    if (!response.ok) {
      const message = await response.text().catch(() => "");
      throw new Error(message || `Backend ingestion failed with status ${response.status}`);
    }
  }
}
