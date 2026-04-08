export type MessageRecord = {
  id: number;
  platform: string;
  externalMessageId: string | null;
  conversationId: number | null;
  conversationTitle: string | null;
  externalConversationId: string | null;
  conversationType: string | null;
  authorId: number | null;
  authorDisplayName: string | null;
  authorUsername: string | null;
  authorExternalUserId: string | null;
  authorPhone: string | null;
  text: string | null;
  messageType: string;
  sentAt: string;
};
