import Fastify, { type FastifyInstance } from "fastify";

import { registerTelegramCollectorRoutes } from "./telegram/telegram-collector.controller.js";
import type { HealthResponse } from "./telegram/telegram-auth.types.js";
import { registerTelegramAuthRoutes } from "./telegram/telegram-auth.controller.js";
import type { TelegramCollectorService } from "./telegram/telegram-collector.service.js";
import type { TelegramAuthService } from "./telegram/telegram-auth.service.js";

interface CreateAppOptions {
  telegramAuthService?: TelegramAuthService;
  telegramCollectorService?: TelegramCollectorService;
}

export function createApp(options: CreateAppOptions = {}): FastifyInstance {
  const app = Fastify({
    logger: false
  });

  app.get<{ Reply: HealthResponse }>("/health", async () => {
    return { status: "ok" };
  });

  if (options.telegramAuthService) {
    registerTelegramAuthRoutes(app, options.telegramAuthService);
  }

  if (options.telegramCollectorService) {
    registerTelegramCollectorRoutes(app, options.telegramCollectorService);
  }

  return app;
}
