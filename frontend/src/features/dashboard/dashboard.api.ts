import type { MessageRecord } from "./dashboard.types";

const DEFAULT_API_BASE_URL = "";

function resolveApiBaseUrl() {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL;
  return configuredBaseUrl?.trim() || DEFAULT_API_BASE_URL;
}

export async function fetchMessages(
  token: string,
  options: { search?: string; platform?: string; limit?: number; offset?: number } = {}
) {
  const searchParams = new URLSearchParams();

  if (options.search?.trim()) {
    searchParams.set("search", options.search.trim());
  }

  if (options.platform?.trim() && options.platform !== "ALL") {
    searchParams.set("platform", options.platform.trim());
  }

  if (typeof options.limit === "number") {
    searchParams.set("limit", String(options.limit));
  }

  if (typeof options.offset === "number" && options.offset > 0) {
    searchParams.set("offset", String(options.offset));
  }

  const query = searchParams.toString();
  const url = `${resolveApiBaseUrl()}/api/messages${query ? `?${query}` : ""}`;

  try {
    const response = await fetch(url, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      if (response.status === 401) {
        return {
          ok: false as const,
          unauthorized: true as const,
          error:
            data && typeof data.error === "string"
              ? data.error
              : "Сессия истекла. Выполните вход заново."
        };
      }

      return {
        ok: false as const,
        unauthorized: false as const,
        error:
          data && typeof data.error === "string"
            ? data.error
            : "Не удалось загрузить сообщения"
      };
    }

    return {
      ok: true as const,
      items: Array.isArray(data) ? (data as MessageRecord[]) : []
    };
  } catch {
    return {
      ok: false as const,
      unauthorized: false as const,
      error: "Сервер недоступен. Проверь backend и повтори запрос."
    };
  }
}
