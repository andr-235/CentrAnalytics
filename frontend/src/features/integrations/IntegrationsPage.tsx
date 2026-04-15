import { useEffect, useState } from "react";

import {
  collectVkGroups,
  deleteVkGroups,
  fetchIntegrationsSnapshot,
  startTelegramSession,
  submitTelegramCode,
  submitTelegramPassword
} from "./integrations.api";
import type {
  IntegrationsResult,
  TelegramActionResult,
  TelegramSessionRecord,
  VkGroupActionResult,
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
  collectVkGroups?: (
    token: string,
    groupIdentifiers: string[]
  ) => Promise<VkGroupActionResult>;
  deleteVkGroups?: (
    token: string,
    groupIdentifiers: string[]
  ) => Promise<VkGroupActionResult>;
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
  collectVkGroups: runVkCollect = collectVkGroups,
  deleteVkGroups: runVkDelete = deleteVkGroups,
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
  const [selectedVkGroupIds, setSelectedVkGroupIds] = useState<string[]>([]);
  const [vkActionMessage, setVkActionMessage] = useState("");
  const [isVkSubmitting, setIsVkSubmitting] = useState(false);

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
    setSelectedVkGroupIds((current) =>
      current.filter((identifier) =>
        result.data.vkGroups.some((group) => String(group.id) === identifier)
      )
    );
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

  function toggleVkGroupSelection(identifier: string) {
    setSelectedVkGroupIds((current) =>
      current.includes(identifier)
        ? current.filter((value) => value !== identifier)
        : [...current, identifier]
    );
  }

  async function runVkAction(
    action: (token: string, groupIdentifiers: string[]) => Promise<VkGroupActionResult>,
    groupIdentifiers: string[]
  ) {
    if (!groupIdentifiers.length) {
      return;
    }

    setIsVkSubmitting(true);
    setError("");
    setVkActionMessage("");
    const result = await action(token, groupIdentifiers);
    setIsVkSubmitting(false);

    if (!result.ok) {
      if (result.unauthorized) {
        onUnauthorized?.();
        return;
      }

      setError(result.error);
      return;
    }

    const unresolvedSuffix = result.unresolvedIdentifiers.length
      ? ` Не найдены: ${result.unresolvedIdentifiers.join(", ")}.`
      : "";
    setVkActionMessage(`${result.message}${unresolvedSuffix}`);
    await refresh();
  }

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
              <span>Контур</span>
              <strong>telegram-auth-gateway</strong>
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
              Telegram работает только через telegram-auth-gateway. После авторизации gateway отправляет события в общую витрину автоматически.
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

          <div className="vk-group-toolbar">
            <div className="vk-group-toolbar__meta">
              <strong>{selectedVkGroupIds.length} выбрано</strong>
              <span>Ручной запуск и удаление работают только по группам из текущей БД.</span>
            </div>
            <div className="vk-group-toolbar__actions">
              <button
                type="button"
                className="integration-action"
                disabled={isVkSubmitting || !selectedVkGroupIds.length}
                onClick={() => void runVkAction(runVkCollect, selectedVkGroupIds)}
              >
                Собрать выбранные
              </button>
              <button
                type="button"
                className="integration-action integration-action--secondary"
                disabled={isVkSubmitting || !selectedVkGroupIds.length}
                onClick={() => void runVkAction(runVkDelete, selectedVkGroupIds)}
              >
                Удалить выбранные
              </button>
            </div>
          </div>

          {vkActionMessage ? (
            <p className="integration-note integration-note--success">{vkActionMessage}</p>
          ) : null}

          <div className="vk-group-list" data-testid="vk-group-scroll">
            <div className="vk-group-list__head">
              <span />
              <span>Группа</span>
              <span>Метод</span>
              <span>Обновлено</span>
            </div>
            {vkGroups.map((group) => {
              const identifier = String(group.id);

              return (
              <div key={group.id} className="vk-group-row">
                <div className="vk-group-row__selection">
                  <label className="vk-group-checkbox">
                    <input
                      type="checkbox"
                      checked={selectedVkGroupIds.includes(identifier)}
                      onChange={() => toggleVkGroupSelection(identifier)}
                      aria-label={`Выбрать группу ${groupTitle(group)}`}
                    />
                  </label>
                </div>
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
                <div className="vk-group-row__meta">
                  <strong>{formatDateTime(group.updatedAt)}</strong>
                  <span>{group.sourceId ? `Source ${group.sourceId}` : "Источник создается"}</span>
                  <div className="vk-group-row__actions">
                    <button
                      type="button"
                      className="integration-action integration-action--ghost"
                      disabled={isVkSubmitting}
                      onClick={() => void runVkAction(runVkCollect, [identifier])}
                    >
                      Собрать
                    </button>
                    <button
                      type="button"
                      className="integration-action integration-action--ghost"
                      disabled={isVkSubmitting}
                      onClick={() => void runVkAction(runVkDelete, [identifier])}
                    >
                      Удалить
                    </button>
                  </div>
                </div>
              </div>
              );
            })}

            {!vkGroups.length ? (
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
