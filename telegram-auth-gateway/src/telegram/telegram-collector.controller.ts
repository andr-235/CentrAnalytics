import type { FastifyInstance } from "fastify";

import type { TelegramCollectorService } from "./telegram-collector.service.js";

export function registerTelegramCollectorRoutes(
  app: FastifyInstance,
  collectorService: TelegramCollectorService
): void {
  app.get("/collector/status", async () => collectorService.getStatus());
}
