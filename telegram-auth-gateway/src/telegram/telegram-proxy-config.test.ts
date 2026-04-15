import { describe, expect, it } from "vitest";

import { buildTelegramProxyConfig } from "./telegram-proxy-config.js";

describe("buildTelegramProxyConfig", () => {
  it("builds a socks5 proxy config", () => {
    const config = buildTelegramProxyConfig({
      enabled: true,
      transport: "socks5",
      host: "centranalytics-tgproxy",
      port: 10808,
      username: "user",
      password: "pass"
    });

    expect(config).toEqual({
      ip: "centranalytics-tgproxy",
      port: 10808,
      socksType: 5,
      username: "user",
      password: "pass"
    });
  });

  it("builds an mtproto proxy config", () => {
    const config = buildTelegramProxyConfig({
      enabled: true,
      transport: "mtproto",
      host: "mtproxy.internal",
      port: 443,
      secret: "secret-value"
    });

    expect(config).toEqual({
      ip: "mtproxy.internal",
      port: 443,
      MTProxy: true,
      secret: "secret-value"
    });
  });

  it("returns undefined when proxy is disabled", () => {
    const config = buildTelegramProxyConfig({
      enabled: false,
      transport: "socks5",
      host: "ignored",
      port: 1080
    });

    expect(config).toBeUndefined();
  });
});
