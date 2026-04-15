import { createApp } from "./app.js";
import { BackendIngestionClient } from "./backend/backend-ingestion.client.js";
import { loadEnv } from "./config/env.js";
import { FileSessionStore } from "./telegram/file-session-store.js";
import { createTelegramClientFactory } from "./telegram/gramjs-client.js";
import { TelegramAuthService } from "./telegram/telegram-auth.service.js";
import { startCollectorInBackground } from "./telegram/telegram-collector-bootstrap.js";
import {
  createGramJsCollectorRuntimeFactory,
  TelegramCollectorService
} from "./telegram/telegram-collector.service.js";

async function main(): Promise<void> {
  const env = loadEnv();
  const repository = new FileSessionStore(env.TELEGRAM_AUTH_DATA_DIR);
  const clientFactory = createTelegramClientFactory({
    apiId: env.TELEGRAM_API_ID,
    apiHash: env.TELEGRAM_API_HASH,
    proxy: env.TELEGRAM_SOCKS5_PROXY_ENABLED
      ? {
          enabled: true,
          host: env.TELEGRAM_SOCKS5_PROXY_HOST,
          port: env.TELEGRAM_SOCKS5_PROXY_PORT,
          username: env.TELEGRAM_SOCKS5_PROXY_USERNAME,
          password: env.TELEGRAM_SOCKS5_PROXY_PASSWORD
        }
      : undefined
  });
  const backendIngestionClient = new BackendIngestionClient(
    env.BACKEND_INGESTION_BASE_URL,
    env.BACKEND_INGESTION_INTERNAL_TOKEN
  );
  let collectorService!: TelegramCollectorService;
  const collectorRuntimeFactory = createGramJsCollectorRuntimeFactory({
    apiId: env.TELEGRAM_API_ID,
    apiHash: env.TELEGRAM_API_HASH,
    backendIngestionClient,
    collectorService: {
      markEventReceived: () => collectorService.markEventReceived(),
      markRuntimeError: (error: unknown) => collectorService.markRuntimeError(error)
    } as TelegramCollectorService,
    proxy: env.TELEGRAM_SOCKS5_PROXY_ENABLED
      ? {
          enabled: true,
          host: env.TELEGRAM_SOCKS5_PROXY_HOST,
          port: env.TELEGRAM_SOCKS5_PROXY_PORT,
          username: env.TELEGRAM_SOCKS5_PROXY_USERNAME,
          password: env.TELEGRAM_SOCKS5_PROXY_PASSWORD
        }
      : undefined
  });
  collectorService = new TelegramCollectorService(repository, collectorRuntimeFactory);
  const telegramAuthService = new TelegramAuthService(repository, clientFactory, {
    onSessionReady: async () => {
      if (env.TELEGRAM_COLLECTOR_ENABLED) {
        await collectorService.onSessionReady();
      }
    },
    onSessionReset: async () => {
      await collectorService.onSessionReset();
    }
  });
  const app = createApp({
    telegramAuthService,
    telegramCollectorService: collectorService
  });

  await app.listen({
    host: env.HOST,
    port: env.PORT
  });

  startCollectorInBackground(env.TELEGRAM_COLLECTOR_ENABLED, collectorService);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
