import { FormEvent, useState } from "react";

import { submitAuthRequest } from "./auth.api";
import type { AuthMode, AuthPayload, SubmitAuth } from "./auth.types";

type AuthPageProps = {
  submitAuth?: SubmitAuth;
  onAuthenticated?: (token: string) => void;
};

const modeMeta: Record<
  AuthMode,
  {
    title: string;
    subtitle: string;
    submitLabel: string;
  }
> = {
  login: {
    title: "Центр аналитики",
    subtitle: "Вход в рабочее пространство мониторинга, интеграций и операционного контроля.",
    submitLabel: "Войти"
  },
  register: {
    title: "Центр аналитики",
    subtitle: "Создание локальной учетной записи без внешних провайдеров и лишних сценариев.",
    submitLabel: "Создать аккаунт"
  }
};

const emptyErrors = {
  username: "",
  password: ""
};

function validate(payload: AuthPayload) {
  return {
    username:
      payload.username.trim().length >= 3 && payload.username.trim().length <= 50
        ? ""
        : "Username должен быть от 3 до 50 символов",
    password:
      payload.password.length >= 6 && payload.password.length <= 100
        ? ""
        : "Пароль должен быть от 6 до 100 символов"
  };
}

export function AuthPage({
  submitAuth = submitAuthRequest,
  onAuthenticated
}: AuthPageProps) {
  const [mode, setMode] = useState<AuthMode>("login");
  const [payload, setPayload] = useState<AuthPayload>({
    username: "",
    password: ""
  });
  const [fieldErrors, setFieldErrors] =
    useState<Partial<Record<keyof AuthPayload, string>>>(emptyErrors);
  const [globalError, setGlobalError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const meta = modeMeta[mode];

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const nextErrors = validate(payload);
    setFieldErrors(nextErrors);
    setGlobalError("");

    if (nextErrors.username || nextErrors.password) {
      return;
    }

    setIsSubmitting(true);

    const result = await submitAuth(mode, {
      username: payload.username.trim(),
      password: payload.password
    });

    if (!result.ok) {
      setFieldErrors({
        username: result.fieldErrors.username ?? "",
        password: result.fieldErrors.password ?? ""
      });
      setGlobalError(result.error ?? "");
      setIsSubmitting(false);
      return;
    }

    localStorage.setItem("centranalytics.token", result.token);
    onAuthenticated?.(result.token);
    setFieldErrors(emptyErrors);
    setGlobalError("");
    setIsSubmitting(false);
  }

  function updateMode(nextMode: AuthMode) {
    setMode(nextMode);
    setFieldErrors(emptyErrors);
    setGlobalError("");
  }

  return (
    <main className="auth-shell">
      <div className="auth-shell__ambient auth-shell__ambient--rose" />
      <div className="auth-shell__ambient auth-shell__ambient--grid" />

      <section className="auth-panel" aria-label="Authentication panel">
        <header className="auth-panel__header">
          <p className="auth-panel__kicker">Закрытый контур</p>
          <h1>{meta.title}</h1>
          <p>{meta.subtitle}</p>
        </header>

        <div className="mode-toggle" role="tablist" aria-label="Auth mode">
          <button
            type="button"
            role="tab"
            aria-selected={mode === "login"}
            className={mode === "login" ? "mode-toggle__item is-active" : "mode-toggle__item"}
            onClick={() => updateMode("login")}
          >
            Вход
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={mode === "register"}
            className={
              mode === "register" ? "mode-toggle__item is-active" : "mode-toggle__item"
            }
            onClick={() => updateMode("register")}
          >
            Регистрация
          </button>
        </div>

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <label className="auth-field">
            <span>Логин</span>
            <input
              name="username"
              type="text"
              autoComplete="username"
              placeholder="operator.name"
              value={payload.username}
              onChange={(event) =>
                setPayload((current) => ({
                  ...current,
                  username: event.target.value
                }))
              }
            />
            {fieldErrors.username ? (
              <span className="auth-field__error">{fieldErrors.username}</span>
            ) : null}
          </label>

          <label className="auth-field">
            <span>Пароль</span>
            <input
              name="password"
              type="password"
              autoComplete={mode === "login" ? "current-password" : "new-password"}
              placeholder="••••••••••"
              value={payload.password}
              onChange={(event) =>
                setPayload((current) => ({
                  ...current,
                  password: event.target.value
                }))
              }
            />
            {fieldErrors.password ? (
              <span className="auth-field__error">{fieldErrors.password}</span>
            ) : null}
          </label>

          {globalError ? (
            <div className="auth-feedback" role="alert" aria-label="auth feedback">
              {globalError}
            </div>
          ) : null}

          <button className="auth-submit" type="submit" disabled={isSubmitting}>
            <span>{isSubmitting ? "Проверяем доступ..." : meta.submitLabel}</span>
          </button>
        </form>
      </section>
    </main>
  );
}
