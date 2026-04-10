import type { OverviewResult, OverviewSnapshot, OverviewWindow } from "./overview.types";

const DEFAULT_API_BASE_URL = "";

function resolveApiBaseUrl() {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL;
  return configuredBaseUrl?.trim() || DEFAULT_API_BASE_URL;
}

export async function fetchOverview(
  token: string,
  window: OverviewWindow
): Promise<OverviewResult> {
  const url = `${resolveApiBaseUrl()}/api/overview?window=${window}`;

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
          ok: false,
          unauthorized: true,
          error:
            data && typeof data.error === "string"
              ? data.error
              : "Сессия истекла. Выполните вход заново."
        };
      }

      return {
        ok: false,
        unauthorized: false,
        error:
          data && typeof data.error === "string"
            ? data.error
            : "Не удалось загрузить обзор"
      };
    }

    return {
      ok: true,
      data: data as OverviewSnapshot
    };
  } catch {
    return {
      ok: false,
      unauthorized: false,
      error: "Сервер недоступен. Проверь backend и повтори запрос."
    };
  }
}
