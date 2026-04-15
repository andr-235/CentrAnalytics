import { z } from "zod";

const TRUE_VALUES = new Set(["1", "true", "yes", "on"]);
const FALSE_VALUES = new Set(["0", "false", "no", "off", ""]);

const envBoolean = (defaultValue: boolean) =>
  z
    .string()
    .optional()
    .transform((value, ctx) => {
      if (value === undefined) {
        return defaultValue;
      }

      const normalized = value.trim().toLowerCase();
      if (TRUE_VALUES.has(normalized)) {
        return true;
      }
      if (FALSE_VALUES.has(normalized)) {
        return false;
      }

      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: `Invalid boolean value: ${value}`
      });
      return z.NEVER;
    });

const proxyTransportSchema = z.enum(["socks5", "mtproto"]);

const envSchema = z.object({
  PORT: z.coerce.number().int().positive().default(8091),
  HOST: z.string().default("0.0.0.0"),
  TELEGRAM_API_ID: z.coerce.number().int().positive(),
  TELEGRAM_API_HASH: z.string().min(1),
  TELEGRAM_AUTH_DATA_DIR: z.string().default("./data"),
  TELEGRAM_SOCKS5_PROXY_ENABLED: envBoolean(false),
  TELEGRAM_PROXY_TRANSPORT: proxyTransportSchema.default("socks5"),
  TELEGRAM_SOCKS5_PROXY_HOST: z.string().default(""),
  TELEGRAM_SOCKS5_PROXY_PORT: z.coerce.number().int().positive().default(1080),
  TELEGRAM_SOCKS5_PROXY_USERNAME: z.string().optional(),
  TELEGRAM_SOCKS5_PROXY_PASSWORD: z.string().optional(),
  TELEGRAM_MTPROTO_PROXY_SECRET: z.string().optional(),
  BACKEND_INGESTION_BASE_URL: z.string().url(),
  BACKEND_INGESTION_INTERNAL_TOKEN: z.string().min(1),
  TELEGRAM_COLLECTOR_ENABLED: envBoolean(true)
});

export type Env = z.infer<typeof envSchema>;

export function loadEnv(source: NodeJS.ProcessEnv = process.env): Env {
  return envSchema.parse(source);
}
