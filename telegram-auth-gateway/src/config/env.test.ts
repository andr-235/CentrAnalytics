import { describe, expect, it } from "vitest";

import { loadEnv } from "./env.js";

function baseEnv(overrides: Record<string, string> = {}): NodeJS.ProcessEnv {
  return {
    TELEGRAM_API_ID: "706692",
    TELEGRAM_API_HASH: "hash",
    BACKEND_INGESTION_BASE_URL: "http://app:8080",
    BACKEND_INGESTION_INTERNAL_TOKEN: "internal-token",
    ...overrides
  };
}

describe("loadEnv", () => {
  it("parses explicit false-like values as false", () => {
    const env = loadEnv(
      baseEnv({
        TELEGRAM_SOCKS5_PROXY_ENABLED: "false",
        TELEGRAM_COLLECTOR_ENABLED: "0"
      })
    );

    expect(env.TELEGRAM_SOCKS5_PROXY_ENABLED).toBe(false);
    expect(env.TELEGRAM_COLLECTOR_ENABLED).toBe(false);
  });

  it("parses explicit true-like values as true", () => {
    const env = loadEnv(
      baseEnv({
        TELEGRAM_SOCKS5_PROXY_ENABLED: "1",
        TELEGRAM_COLLECTOR_ENABLED: "true"
      })
    );

    expect(env.TELEGRAM_SOCKS5_PROXY_ENABLED).toBe(true);
    expect(env.TELEGRAM_COLLECTOR_ENABLED).toBe(true);
  });

  it("defaults proxy transport to socks5", () => {
    const env = loadEnv(baseEnv());

    expect(env.TELEGRAM_PROXY_TRANSPORT).toBe("socks5");
  });

  it("parses mtproto proxy transport settings", () => {
    const env = loadEnv(
      baseEnv({
        TELEGRAM_PROXY_TRANSPORT: "mtproto",
        TELEGRAM_SOCKS5_PROXY_ENABLED: "true",
        TELEGRAM_SOCKS5_PROXY_HOST: "mtproxy.internal",
        TELEGRAM_SOCKS5_PROXY_PORT: "443",
        TELEGRAM_MTPROTO_PROXY_SECRET: "secret-value"
      })
    );

    expect(env.TELEGRAM_PROXY_TRANSPORT).toBe("mtproto");
    expect(env.TELEGRAM_MTPROTO_PROXY_SECRET).toBe("secret-value");
  });
});
