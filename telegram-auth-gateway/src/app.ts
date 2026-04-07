import Fastify, { type FastifyInstance } from "fastify";

import type { HealthResponse } from "./telegram/telegram-auth.types.js";
import { registerTelegramAuthRoutes } from "./telegram/telegram-auth.controller.js";
import type { TelegramAuthService } from "./telegram/telegram-auth.service.js";

interface CreateAppOptions {
  telegramAuthService?: TelegramAuthService;
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

  return app;
}
