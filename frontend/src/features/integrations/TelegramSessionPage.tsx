import { useEffect, useState } from "react";

import {
  fetchIntegrationsSnapshot,
  startTelegramSession,
  submitTelegramCode,
  submitTelegramPassword
} from "./integrations.api";
import type {
  IntegrationsResult,
  TelegramActionResult,
  TelegramSessionRecord
} from "./integrations.types";

type TelegramSessionPageProps = {
  token: string;
  loadSnapshot?: (token: string) => Promise<IntegrationsResult>;
  startSession?: (token: string, phoneNumber: string) => Promise<TelegramActionResult>;
  submitCode?: (
    token: string,
    sessionId: string,
    code: string
  ) => Promise<TelegramActionResult>;
  submitPassword?: (
    token: string,
    sessionId: string,
    password: string
  ) => Promise<TelegramActionResult>;
  onUnauthorized?: () => void;
};

function formatDateTime(value: string | null) {
  if (!value) {
    return "Еще не синхронизировалось";
  }

  return new Intl.DateTimeFormat("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
}

function telegramStateLabel(session: TelegramSessionRecord | null) {
  if (!session) {
    return "Сессия не запущена";
  }

  switch (session.state) {
    case "READY":
      return "Готова к приему";
    case "WAIT_CODE":
      return "Ожидает код";
    case "WAIT_PASSWORD":
      return "Ожидает пароль";
    case "FAILED":
      return "Ошибка сессии";
    default:
      return "Инициализация";
  }
}

function telegramStateTone(session: TelegramSessionRecord | null) {
  if (!session) {
    return "idle";
  }

  switch (session.state) {
    case "READY":
      return "live";
    case "FAILED":
      return "danger";
    default:
      return "pending";
  }
}

export function TelegramSessionPage({
  token,
  loadSnapshot = fetchIntegrationsSnapshot,
  startSession = startTelegramSession,
  submitCode = submitTelegramCode,
  submitPassword = submitTelegramPassword,
  onUnauthorized
}: TelegramSessionPageProps) {
  const [telegramSession, setTelegramSession] = useState<TelegramSessionRecord | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [phoneNumber, setPhoneNumber] = useState("");
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");

  async function refresh() {
    setIsLoading(true);
    setError("");

    const result = await loadSnapshot(token);

    if (!result.ok) {
      if (result.unauthorized) {
        onUnauthorized?.();
        return;
      }

      setError(result.error);
      setTelegramSession(null);
      setIsLoading(false);
      return;
    }

    setTelegramSession(result.data.telegramSession);
    setIsLoading(false);
  }

  useEffect(() => {
    void refresh();
  }, []);

  async function runTelegramAction(action: Promise<TelegramActionResult>) {
    setIsSubmitting(true);
    setError("");
    const result = await action;
    setIsSubmitting(false);

    if (!result.ok) {
      if (result.unauthorized) {
        onUnauthorized?.();
        return;
      }

      setError(result.error);
      return;
    }

    setTelegramSession(result.session);
    setCode("");
    setPassword("");
  }

  return (
    <main className="integrations-shell">
      <section className="integrations-hero">
        <div>
          <p className="integrations-hero__eyebrow">Платформа</p>
          <h1>Telegram</h1>
          <p>
            Управление пользовательской сессией Telegram в отдельном платформенном
            разделе.
          </p>
        </div>

        <button
          type="button"
          className="integrations-refresh"
          onClick={() => void refresh()}
        >
          {isLoading ? "Обновление..." : "Обновить статус"}
        </button>
      </section>

      {error ? (
        <div className="dashboard-error" role="alert">
          {error}
        </div>
      ) : null}

      <article className="integration-panel integration-panel--telegram">
        <header className="integration-panel__header">
          <div>
            <p className="integration-panel__eyebrow">Пользовательская сессия</p>
            <h2>Telegram</h2>
          </div>
          <span className={`state-badge state-badge--${telegramStateTone(telegramSession)}`}>
            {telegramStateLabel(telegramSession)}
          </span>
        </header>

        <div className="integration-metrics">
          <div className="integration-metric">
            <span>Телефон</span>
            <strong>{telegramSession?.phoneNumber || "Не задан"}</strong>
          </div>
          <div className="integration-metric">
            <span>Состояние</span>
            <strong>{telegramSession?.state || "NOT_STARTED"}</strong>
          </div>
          <div className="integration-metric">
            <span>Последний sync</span>
            <strong>{formatDateTime(telegramSession?.lastSyncAt ?? null)}</strong>
          </div>
          <div className="integration-metric">
            <span>Аккаунт</span>
            <strong>
              {telegramSession?.telegramUserId
                ? `ID ${telegramSession.telegramUserId}`
                : "Еще не подтвержден"}
            </strong>
          </div>
        </div>

        <div className="integration-actions">
          <label className="integration-field">
            <span>Запуск сессии</span>
            <input
              type="tel"
              placeholder="+79991234567"
              value={phoneNumber}
              onChange={(event) => setPhoneNumber(event.target.value)}
            />
          </label>
          <button
            type="button"
            className="integration-action"
            disabled={isSubmitting || !phoneNumber.trim()}
            onClick={() => void runTelegramAction(startSession(token, phoneNumber.trim()))}
          >
            Запустить сессию
          </button>

          <label className="integration-field">
            <span>Код подтверждения</span>
            <input
              type="text"
              placeholder="12345"
              value={code}
              onChange={(event) => setCode(event.target.value)}
            />
          </label>
          <button
            type="button"
            className="integration-action integration-action--secondary"
            disabled={isSubmitting || !telegramSession?.id || !code.trim()}
            onClick={() =>
              telegramSession
                ? void runTelegramAction(submitCode(token, telegramSession.id, code.trim()))
                : undefined
            }
          >
            Отправить код
          </button>

          <label className="integration-field">
            <span>Пароль Telegram</span>
            <input
              type="password"
              placeholder="Пароль 2FA"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>
          <button
            type="button"
            className="integration-action integration-action--secondary"
            disabled={isSubmitting || !telegramSession?.id || !password.trim()}
            onClick={() =>
              telegramSession
                ? void runTelegramAction(
                    submitPassword(token, telegramSession.id, password.trim())
                  )
                : undefined
            }
          >
            Отправить пароль
          </button>
        </div>

        {telegramSession?.errorMessage ? (
          <p className="integration-note integration-note--danger">
            {telegramSession.errorMessage}
          </p>
        ) : (
          <p className="integration-note">
            Telegram работает через user-session. После авторизации поток сообщений
            идет в общую витрину автоматически.
          </p>
        )}
      </article>
    </main>
  );
}
