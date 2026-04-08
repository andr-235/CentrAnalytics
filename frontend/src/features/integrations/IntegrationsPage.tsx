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
  TelegramSessionRecord,
  VkGroupRecord
} from "./integrations.types";

type IntegrationsPageProps = {
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

function groupTitle(group: VkGroupRecord) {
  return group.name?.trim() || group.screenName?.trim() || `VK group ${group.vkGroupId}`;
}

export function IntegrationsPage({
  token,
  loadSnapshot = fetchIntegrationsSnapshot,
  startSession = startTelegramSession,
  submitCode = submitTelegramCode,
  submitPassword = submitTelegramPassword,
  onUnauthorized
}: IntegrationsPageProps) {
  const [telegramSession, setTelegramSession] = useState<TelegramSessionRecord | null>(null);
  const [vkGroups, setVkGroups] = useState<VkGroupRecord[]>([]);
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
      setVkGroups([]);
      setIsLoading(false);
      return;
    }

    setTelegramSession(result.data.telegramSession);
    setVkGroups(result.data.vkGroups);
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

  const recentGroups = vkGroups.slice(0, 6);

  return (
    <main className="integrations-shell">
      <section className="integrations-hero">
        <div>
          <p className="integrations-hero__eyebrow">Операционный контур</p>
          <h1>Интеграции</h1>
          <p>
            Управление потоками Telegram и VK, ручной контроль авторизации и
            обзор регионального сбора без лишней служебной перегрузки.
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

      <section className="integrations-grid">
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
              Telegram работает через user-session. После авторизации поток
              сообщений идет в общую витрину автоматически.
            </p>
          )}
        </article>

        <article className="integration-panel integration-panel--vk">
          <header className="integration-panel__header">
            <div>
              <p className="integration-panel__eyebrow">Региональный сбор</p>
              <h2>VK</h2>
            </div>
            <span className="state-badge state-badge--live">
              {vkGroups.length} групп
            </span>
          </header>

          <div className="integration-metrics">
            <div className="integration-metric">
              <span>Режим</span>
              <strong>Автосбор по региону</strong>
            </div>
            <div className="integration-metric">
              <span>Статус</span>
              <strong>Управляется сервером</strong>
            </div>
            <div className="integration-metric">
              <span>Найдено групп</span>
              <strong>{vkGroups.length}</strong>
            </div>
            <div className="integration-metric">
              <span>Последнее обновление</span>
              <strong>
                {vkGroups[0] ? formatDateTime(vkGroups[0].updatedAt) : "Пока нет данных"}
              </strong>
            </div>
          </div>

          <p className="integration-note">
            Регион, лимиты и интервал автосбора сейчас задаются на сервере через
            runtime-конфиг. Экран показывает найденные группы и текущее
            состояние витрины без декоративного шума.
          </p>

          <div className="vk-group-list">
            <div className="vk-group-list__head">
              <span>Группа</span>
              <span>Метод</span>
              <span>Обновлено</span>
            </div>
            {recentGroups.map((group) => (
              <div key={group.id} className="vk-group-row">
                <div>
                  <strong>{groupTitle(group)}</strong>
                  <span>
                    {group.screenName ? `vk.com/${group.screenName}` : `ID ${group.vkGroupId}`}
                  </span>
                </div>
                <div>
                  <strong>{group.collectionMethod}</strong>
                  <span>{group.regionMatchSource}</span>
                </div>
                <div>
                  <strong>{formatDateTime(group.updatedAt)}</strong>
                  <span>{group.sourceId ? `Source ${group.sourceId}` : "Источник создается"}</span>
                </div>
              </div>
            ))}

            {!recentGroups.length ? (
              <div className="vk-group-list__empty">
                Региональные группы пока не найдены. После первого цикла
                автосбора список появится здесь.
              </div>
            ) : null}
          </div>
        </article>
      </section>
    </main>
  );
}
