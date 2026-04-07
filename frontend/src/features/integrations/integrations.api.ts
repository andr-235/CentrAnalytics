import type {
  IntegrationsResult,
  TelegramActionResult,
  TelegramSessionRecord,
  VkGroupRecord
} from "./integrations.types";

const DEFAULT_API_BASE_URL = "";

function resolveApiBaseUrl() {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL;
  return configuredBaseUrl?.trim() || DEFAULT_API_BASE_URL;
}

async function readJson(response: Response) {
  return response.json().catch(() => null);
}

function authHeaders(token: string, includeJson = false) {
  return {
    Authorization: `Bearer ${token}`,
    ...(includeJson ? { "Content-Type": "application/json" } : {})
  };
}

export async function fetchIntegrationsSnapshot(
  token: string
): Promise<IntegrationsResult> {
  try {
    const [telegramResponse, vkGroupsResponse] = await Promise.all([
      fetch(`${resolveApiBaseUrl()}/api/admin/integrations/telegram-user/current`, {
        headers: authHeaders(token)
      }),
      fetch(`${resolveApiBaseUrl()}/api/admin/integrations/vk/groups`, {
        headers: authHeaders(token)
      })
    ]);

    const telegramData = await readJson(telegramResponse);
    const vkGroupsData = await readJson(vkGroupsResponse);

    const telegramSession =
      telegramResponse.ok && telegramData && typeof telegramData === "object"
        ? (telegramData as TelegramSessionRecord)
        : null;

    if (!vkGroupsResponse.ok) {
      return {
        ok: false,
        error: "Не удалось загрузить состояние интеграций"
      };
    }

    return {
      ok: true,
      data: {
        telegramSession,
        vkGroups: Array.isArray(vkGroupsData) ? (vkGroupsData as VkGroupRecord[]) : []
      }
    };
  } catch {
    return {
      ok: false,
      error: "Сервер недоступен. Проверь backend и повтори запрос."
    };
  }
}

async function submitTelegramAction(
  token: string,
  path: string,
  payload: object
): Promise<TelegramActionResult> {
  try {
    const response = await fetch(`${resolveApiBaseUrl()}${path}`, {
      method: "POST",
      headers: authHeaders(token, true),
      body: JSON.stringify(payload)
    });

    const data = await readJson(response);

    if (!response.ok || !data || typeof data !== "object") {
      return {
        ok: false,
        error:
          data && typeof data.error === "string"
            ? data.error
            : "Не удалось выполнить действие Telegram"
      };
    }

    return {
      ok: true,
      session: data as TelegramSessionRecord
    };
  } catch {
    return {
      ok: false,
      error: "Сервер недоступен. Проверь backend и повтори запрос."
    };
  }
}

export function startTelegramSession(token: string, phoneNumber: string) {
  return submitTelegramAction(token, "/api/admin/integrations/telegram-user/start", {
    phoneNumber
  });
}

export function submitTelegramCode(token: string, sessionId: string, code: string) {
  return submitTelegramAction(
    token,
    `/api/admin/integrations/telegram-user/${sessionId}/code`,
    { code }
  );
}

export function submitTelegramPassword(token: string, sessionId: string, password: string) {
  return submitTelegramAction(
    token,
    `/api/admin/integrations/telegram-user/${sessionId}/password`,
    { password }
  );
}
