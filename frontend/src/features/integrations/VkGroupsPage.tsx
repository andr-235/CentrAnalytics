import { useEffect, useState } from "react";

import {
  collectVkGroups,
  deleteVkGroups,
  fetchIntegrationsSnapshot
} from "./integrations.api";
import type {
  IntegrationsResult,
  VkGroupActionResult,
  VkGroupRecord
} from "./integrations.types";

type VkGroupsPageProps = {
  token: string;
  loadSnapshot?: (token: string) => Promise<IntegrationsResult>;
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

function groupTitle(group: VkGroupRecord) {
  return group.name?.trim() || group.screenName?.trim() || `VK group ${group.vkGroupId}`;
}

export function VkGroupsPage({
  token,
  loadSnapshot = fetchIntegrationsSnapshot,
  collectVkGroups: runVkCollect = collectVkGroups,
  deleteVkGroups: runVkDelete = deleteVkGroups,
  onUnauthorized
}: VkGroupsPageProps) {
  const [vkGroups, setVkGroups] = useState<VkGroupRecord[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
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
      setVkGroups([]);
      setIsLoading(false);
      return;
    }

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
          <p className="integrations-hero__eyebrow">Платформа</p>
          <h1>Вконтакте</h1>
          <p>Работа с найденными группами и ручным запуском регионального сбора.</p>
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

      <article className="integration-panel integration-panel--vk">
        <header className="integration-panel__header">
          <div>
            <p className="integration-panel__eyebrow">Региональный сбор</p>
            <h2>VK</h2>
          </div>
          <span className="state-badge state-badge--live">{vkGroups.length} групп</span>
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
            <strong>{vkGroups[0] ? formatDateTime(vkGroups[0].updatedAt) : "Пока нет данных"}</strong>
          </div>
        </div>

        <p className="integration-note">
          Регион, лимиты и интервал автосбора сейчас задаются на сервере через
          runtime-конфиг. Экран показывает найденные группы и текущее состояние
          витрины без декоративного шума.
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
              Региональные группы пока не найдены. После первого цикла автосбора
              список появится здесь.
            </div>
          ) : null}
        </div>
      </article>
    </main>
  );
}
