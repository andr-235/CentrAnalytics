import type { TelegramCollectorService } from "./telegram-collector.service.js";

export function startCollectorInBackground(
  enabled: boolean,
  collectorService: Pick<TelegramCollectorService, "startFromCurrentSession">
): void {
  if (!enabled) {
    return;
  }

  void collectorService.startFromCurrentSession().catch(() => undefined);
}
