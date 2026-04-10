export type OverviewWindow = "24h" | "7d" | "30d";

export type OverviewHighlight = {
  label: string;
  value: string;
};

export type OverviewTrendPoint = {
  timestamp: string;
  messageCount: number;
};

export type PlatformIntegrationStatus = {
  syncStatus: string;
  lastEventAt: string | null;
  lastSuccessAt: string | null;
  lastErrorMessage: string | null;
  sourceCount: number;
  detail: string;
};

export type PlatformAttentionItem = {
  tone: string;
  message: string;
};

export type PlatformOverview = {
  platform: "TELEGRAM" | "VK" | "WHATSAPP" | "MAX";
  label: string;
  status: "healthy" | "warning" | "critical" | "inactive";
  highlights: OverviewHighlight[];
  trend: OverviewTrendPoint[];
  integration: PlatformIntegrationStatus;
  attentionItems: PlatformAttentionItem[];
};

export type OverviewSnapshot = {
  generatedAt: string;
  window: OverviewWindow;
  summary: {
    messageCount: number;
    conversationCount: number;
    activeAuthorCount: number;
    platformIssueCount: number;
  };
  platforms: PlatformOverview[];
};

export type OverviewResult =
  | { ok: true; data: OverviewSnapshot }
  | { ok: false; unauthorized: true; error: string }
  | { ok: false; unauthorized: false; error: string };
