import { useEffect, useState } from "react";

import { fetchOverview } from "./overview.api";
import type {
  OverviewHighlight,
  OverviewResult,
  OverviewSnapshot,
  OverviewWindow,
  PlatformAttentionItem,
  PlatformOverview
} from "./overview.types";

type OverviewPageProps = {
  token: string;
  loadOverview?: (token: string, window: OverviewWindow) => Promise<OverviewResult>;
  onUnauthorized?: () => void;
};

const windows: Array<{ value: OverviewWindow; label: string }> = [
  { value: "24h", label: "24ч" },
  { value: "7d", label: "7д" },
  { value: "30d", label: "30д" }
];

function formatNumber(value: number) {
  return new Intl.NumberFormat("ru-RU").format(value);
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "Нет данных";
  }

  return new Intl.DateTimeFormat("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <article className="overview-summary-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function HighlightItem({ item }: { item: OverviewHighlight }) {
  return (
    <li className="overview-highlight">
      <span>{item.label}</span>
      <strong>{item.value}</strong>
    </li>
  );
}

function AttentionItem({ item }: { item: PlatformAttentionItem }) {
  return <li className={`overview-attention-item is-${item.tone}`}>{item.message}</li>;
}

function TrendBars({ platform }: { platform: PlatformOverview }) {
  if (!platform.trend.length) {
    return <div className="overview-trend overview-trend--empty">Нет данных за период</div>;
  }

  const maxValue = Math.max(...platform.trend.map((point) => point.messageCount), 1);

  return (
    <div className="overview-trend" aria-label={`${platform.label} тренд сообщений`}>
      {platform.trend.map((point) => (
        <div key={`${platform.platform}-${point.timestamp}`} className="overview-trend__point">
          <span
            className="overview-trend__bar"
            style={{ height: `${Math.max(14, (point.messageCount / maxValue) * 82)}px` }}
            title={`${formatDateTime(point.timestamp)} · ${point.messageCount}`}
          />
        </div>
      ))}
    </div>
  );
}

function PlatformSection({ platform }: { platform: PlatformOverview }) {
  return (
    <section className={`overview-platform overview-platform--${platform.status}`}>
      <div className="overview-platform__header">
        <div>
          <p className="overview-platform__eyebrow">Платформенный срез</p>
          <h2>{platform.label}</h2>
        </div>
        <span className={`overview-status overview-status--${platform.status}`}>
          {platform.status}
        </span>
      </div>

      <div className="overview-platform__grid">
        <div className="overview-panel">
          <p className="overview-panel__label">KPI</p>
          <ul className="overview-highlights">
            {platform.highlights.map((item) => (
              <HighlightItem key={`${platform.platform}-${item.label}`} item={item} />
            ))}
          </ul>
        </div>

        <div className="overview-panel">
          <p className="overview-panel__label">Динамика</p>
          <TrendBars platform={platform} />
        </div>

        <div className="overview-panel">
          <p className="overview-panel__label">Интеграция</p>
          <dl className="overview-integration">
            <div>
              <dt>Статус</dt>
              <dd>{platform.integration.detail}</dd>
            </div>
            <div>
              <dt>Источники</dt>
              <dd>{formatNumber(platform.integration.sourceCount)}</dd>
            </div>
            <div>
              <dt>Последнее событие</dt>
              <dd>{formatDateTime(platform.integration.lastEventAt)}</dd>
            </div>
            <div>
              <dt>Последний успех</dt>
              <dd>{formatDateTime(platform.integration.lastSuccessAt)}</dd>
            </div>
          </dl>
          {platform.integration.lastErrorMessage ? (
            <p className="overview-panel__error">{platform.integration.lastErrorMessage}</p>
          ) : null}
        </div>

        <div className="overview-panel">
          <p className="overview-panel__label">Требует внимания</p>
          <ul className="overview-attention">
            {platform.attentionItems.map((item, index) => (
              <AttentionItem key={`${platform.platform}-${index}`} item={item} />
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}

export function OverviewPage({
  token,
  loadOverview = fetchOverview,
  onUnauthorized
}: OverviewPageProps) {
  const [window, setWindow] = useState<OverviewWindow>("24h");
  const [snapshot, setSnapshot] = useState<OverviewSnapshot | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  async function refresh(nextWindow = window) {
    setIsLoading(true);
    setError("");

    const result = await loadOverview(token, nextWindow);

    if (!result.ok) {
      if (result.unauthorized) {
        onUnauthorized?.();
        return;
      }

      setSnapshot(null);
      setError(result.error);
      setIsLoading(false);
      return;
    }

    setSnapshot(result.data);
    setWindow(nextWindow);
    setIsLoading(false);
  }

  useEffect(() => {
    void refresh("24h");
  }, []);

  return (
    <main className="overview-shell">
      <section className="overview-hero">
        <div>
          <p className="overview-hero__eyebrow">Операционный обзор</p>
          <h1>Обзор</h1>
          <p>
            Сводный экран по трафику, состоянию интеграций и приоритетным сигналам
            по платформам.
          </p>
        </div>

        <div className="overview-toolbar">
          <div className="overview-window-switch" role="group" aria-label="Период обзора">
            {windows.map((option) => (
              <button
                key={option.value}
                type="button"
                className={
                  option.value === window
                    ? "overview-window-switch__button is-active"
                    : "overview-window-switch__button"
                }
                onClick={() => void refresh(option.value)}
              >
                {option.label}
              </button>
            ))}
          </div>

          <div className="overview-toolbar__meta">
            <span>
              Обновлено: {snapshot ? formatDateTime(snapshot.generatedAt) : "Нет данных"}
            </span>
            <button
              type="button"
              className="dashboard-refresh"
              onClick={() => void refresh(window)}
            >
              {isLoading ? "Загрузка..." : "Обновить"}
            </button>
          </div>
        </div>
      </section>

      {error ? (
        <div className="dashboard-error" role="alert">
          {error}
        </div>
      ) : null}

      {snapshot ? (
        <>
          <section className="overview-summary-grid">
            <SummaryCard label="Сообщения" value={formatNumber(snapshot.summary.messageCount)} />
            <SummaryCard label="Диалоги" value={formatNumber(snapshot.summary.conversationCount)} />
            <SummaryCard label="Авторы" value={formatNumber(snapshot.summary.activeAuthorCount)} />
            <SummaryCard
              label="Платформы с проблемами"
              value={formatNumber(snapshot.summary.platformIssueCount)}
            />
          </section>

          <div className="overview-platforms">
            {snapshot.platforms.map((platform) => (
              <PlatformSection key={platform.platform} platform={platform} />
            ))}
          </div>
        </>
      ) : null}

      {!isLoading && !snapshot && !error ? (
        <div className="dashboard-empty">Обзор пока недоступен.</div>
      ) : null}
    </main>
  );
}
