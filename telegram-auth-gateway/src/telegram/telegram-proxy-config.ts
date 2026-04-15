export type TelegramProxyTransport = "socks5" | "mtproto";

export interface TelegramProxyOptions {
  enabled: boolean;
  transport: TelegramProxyTransport;
  host: string;
  port: number;
  username?: string;
  password?: string;
  secret?: string;
}

export interface GramJsSocks5ProxyConfig {
  ip: string;
  port: number;
  socksType: 5;
  username?: string;
  password?: string;
}

export interface GramJsMtprotoProxyConfig {
  ip: string;
  port: number;
  MTProxy: true;
  secret: string;
}

export type GramJsProxyConfig = GramJsSocks5ProxyConfig | GramJsMtprotoProxyConfig;

export function buildTelegramProxyConfig(options: TelegramProxyOptions): GramJsProxyConfig | undefined {
  if (!options.enabled) {
    return undefined;
  }

  if (options.transport === "mtproto") {
    if (!options.secret) {
      throw new Error("TELEGRAM_MTPROTO_PROXY_SECRET is required when TELEGRAM_PROXY_TRANSPORT=mtproto");
    }

    return {
      ip: options.host,
      port: options.port,
      MTProxy: true,
      secret: options.secret
    };
  }

  return {
    ip: options.host,
    port: options.port,
    socksType: 5,
    username: options.username,
    password: options.password
  };
}
