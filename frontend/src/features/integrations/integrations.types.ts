export type TelegramSessionState =
  | "WAIT_CODE"
  | "WAIT_PASSWORD"
  | "READY"
  | "FAILED"
  | "CREATED";

export type TelegramSessionRecord = {
  id: string;
  phoneNumber: string;
  telegramUserId: number | null;
  state: TelegramSessionState;
  authorized: boolean;
  errorMessage: string | null;
  lastSyncAt: string | null;
};

export type VkGroupRecord = {
  id: number;
  vkGroupId: number;
  sourceId: number | null;
  screenName: string | null;
  name: string | null;
  regionMatchSource: string;
  collectionMethod: string;
  rawJson: string;
  updatedAt: string;
};

export type IntegrationsSnapshot = {
  telegramSession: TelegramSessionRecord | null;
  vkGroups: VkGroupRecord[];
};

export type IntegrationsResult =
  | { ok: true; data: IntegrationsSnapshot }
  | { ok: false; error: string; unauthorized?: boolean };

export type TelegramActionResult =
  | { ok: true; session: TelegramSessionRecord }
  | { ok: false; error: string; unauthorized?: boolean };
