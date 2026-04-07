export type MessageRecord = {
  id: number;
  platform: string;
  externalMessageId: string | null;
  conversationId: number | null;
  authorId: number | null;
  text: string | null;
  messageType: string;
  sentAt: string;
};
