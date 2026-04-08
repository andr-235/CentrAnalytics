export type TelegramCollectorState = "STOPPED" | "STARTING" | "RUNNING" | "FAILED";

export interface TelegramCollectorStatus {
  state: TelegramCollectorState;
  lastEventAt: string | null;
  lastError: string | null;
  selfUserId: number | null;
  selfUsername: string | null;
}
