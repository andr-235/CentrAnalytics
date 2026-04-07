import { z } from "zod";

const envSchema = z.object({
  PORT: z.coerce.number().int().positive().default(8091),
  HOST: z.string().default("0.0.0.0"),
  TELEGRAM_API_ID: z.coerce.number().int().positive(),
  TELEGRAM_API_HASH: z.string().min(1),
  TELEGRAM_AUTH_DATA_DIR: z.string().default("./data"),
  TELEGRAM_SOCKS5_PROXY_ENABLED: z.coerce.boolean().default(false),
  TELEGRAM_SOCKS5_PROXY_HOST: z.string().default(""),
  TELEGRAM_SOCKS5_PROXY_PORT: z.coerce.number().int().positive().default(1080),
  TELEGRAM_SOCKS5_PROXY_USERNAME: z.string().optional(),
  TELEGRAM_SOCKS5_PROXY_PASSWORD: z.string().optional()
});

export type Env = z.infer<typeof envSchema>;

export function loadEnv(source: NodeJS.ProcessEnv = process.env): Env {
  return envSchema.parse(source);
}
