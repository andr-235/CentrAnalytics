import type { AuthMode, AuthPayload, AuthResult } from "./auth.types";

const DEFAULT_API_BASE_URL = "";

const endpointByMode: Record<AuthMode, string> = {
  login: "/auth/login",
  register: "/auth/register"
};

function resolveApiBaseUrl() {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL;
  return configuredBaseUrl?.trim() || DEFAULT_API_BASE_URL;
}

export async function submitAuthRequest(
  mode: AuthMode,
  payload: AuthPayload
): Promise<AuthResult> {
  try {
    const response = await fetch(
      `${resolveApiBaseUrl()}${endpointByMode[mode]}`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
      }
    );

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      if (data && typeof data === "object" && !("error" in data)) {
        return {
          ok: false,
          fieldErrors: {
            username:
              typeof data.username === "string" ? data.username : undefined,
            password:
              typeof data.password === "string" ? data.password : undefined
          }
        };
      }

      return {
        ok: false,
        error:
          data && typeof data.error === "string"
            ? data.error
            : "Unable to complete authentication",
        fieldErrors: {}
      };
    }

    if (!data || typeof data.token !== "string") {
      return {
        ok: false,
        error: "Backend returned an invalid auth response",
        fieldErrors: {}
      };
    }

    return {
      ok: true,
      token: data.token
    };
  } catch {
    return {
      ok: false,
      error: "Network error. Check backend availability and try again.",
      fieldErrors: {}
    };
  }
}
