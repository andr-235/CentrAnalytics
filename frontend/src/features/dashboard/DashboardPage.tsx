import { useEffect, useState } from "react";

import { fetchMessages } from "./dashboard.api";
import type { MessageRecord } from "./dashboard.types";

type DashboardPageProps = {
  token: string;
  loadMessages?: typeof fetchMessages;
  onUnauthorized?: () => void;
};

const platformOptions = ["ALL", "TELEGRAM", "WHATSAPP", "VK"];
const PAGE_SIZE = 25;
const pageSizeOptions = [25, 50, 100];

function formatDateTime(value: string) {
  const date = new Date(value);
  return new Intl.DateTimeFormat("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

function platformLabel(value: string) {
  switch (value) {
    case "TELEGRAM":
      return "Telegram";
    case "WHATSAPP":
      return "WhatsApp";
    case "VK":
      return "VK";
    default:
      return value;
  }
}

function authorPrimary(item: MessageRecord) {
  return item.authorDisplayName || "Неизвестный автор";
}

function authorSecondary(item: MessageRecord) {
  if (item.platform === "TELEGRAM") {
    const username = item.authorUsername?.trim();
    const externalUserId = item.authorExternalUserId?.trim();

    if (username && externalUserId) {
      return `@${username} · id ${externalUserId}`;
    }

    if (username) {
      return `@${username}`;
    }

    if (item.authorPhone && externalUserId) {
      return `${item.authorPhone} · id ${externalUserId}`;
    }

    if (externalUserId) {
      return `id ${externalUserId}`;
    }
  }

  return item.authorPhone || null;
}

function conversationPrimary(item: MessageRecord) {
  if (item.platform === "TELEGRAM") {
    return item.conversationTitle?.trim() || item.externalConversationId || item.conversationType || "Диалог";
  }

  const title = item.conversationTitle?.trim();
  const author = item.authorDisplayName?.trim();

  if (title && title !== author) {
    return title;
  }

  return item.conversationType || "Диалог";
}

function conversationSecondary(item: MessageRecord) {
  if (item.platform === "TELEGRAM") {
    if (item.conversationType && item.externalConversationId) {
      return `${item.conversationType} · ${item.externalConversationId}`;
    }

    return item.conversationType || item.externalConversationId || null;
  }

  const title = item.conversationTitle?.trim();
  const author = item.authorDisplayName?.trim();

  if (title && title !== author) {
    return item.conversationType || item.externalConversationId || null;
  }

  return item.externalConversationId || null;
}

export function DashboardPage({
  token,
  loadMessages = fetchMessages,
  onUnauthorized
}: DashboardPageProps) {
  const [items, setItems] = useState<MessageRecord[]>([]);
  const [search, setSearch] = useState("");
  const [platform, setPlatform] = useState("ALL");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(PAGE_SIZE);
  const [isLoading, setIsLoading] = useState(true);
  const [hasNextPage, setHasNextPage] = useState(false);
  const [error, setError] = useState("");

  async function refresh(
    nextSearch = search,
    nextPlatform = platform,
    nextPage = page,
    nextPageSize = pageSize
  ) {
    setIsLoading(true);
    setError("");

    const result = await loadMessages(token, {
      limit: nextPageSize,
      offset: nextPage * nextPageSize,
      search: nextSearch,
      platform: nextPlatform
    });

    if (!result.ok) {
      if (result.unauthorized) {
        onUnauthorized?.();
        return;
      }

      setItems([]);
      setError(result.error);
      setIsLoading(false);
      return;
    }

    setItems(result.items);
    setPage(nextPage);
    setPageSize(nextPageSize);
    setHasNextPage(result.items.length === nextPageSize);
    setIsLoading(false);
  }

  useEffect(() => {
    void refresh("", "ALL", 0, PAGE_SIZE);
  }, []);

  return (
    <main className="dashboard-shell">
      <section className="dashboard-hero">
        <div>
          <p className="dashboard-hero__eyebrow">Операционный журнал</p>
          <h1>Сообщения</h1>
          <p>
            Рабочая таблица событий по входящим сообщениям, авторам и диалогам.
          </p>
        </div>

        <div className="dashboard-toolbar">
          <label className="dashboard-search">
            <span className="sr-only">Поиск</span>
            <input
              type="search"
              placeholder="Поиск по тексту"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </label>

          <label className="dashboard-select">
            <span>Платформа</span>
            <select
              aria-label="Платформа"
              value={platform}
              onChange={(event) => setPlatform(event.target.value)}
            >
              {platformOptions.map((option) => (
                <option key={option} value={option}>
                  {option === "ALL" ? "Все" : platformLabel(option)}
                </option>
              ))}
            </select>
          </label>

          <label className="dashboard-select">
            <span>Размер страницы</span>
            <select
              aria-label="Размер страницы"
              value={String(pageSize)}
              onChange={(event) => {
                const nextPageSize = Number(event.target.value);
                void refresh(search, platform, 0, nextPageSize);
              }}
            >
              {pageSizeOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>

          <button
            type="button"
            className="dashboard-refresh"
            onClick={() => void refresh(search, platform, 0, pageSize)}
          >
            {isLoading ? "Загрузка..." : "Обновить"}
          </button>
        </div>
      </section>

      {error ? (
        <div className="dashboard-error" role="alert">
          {error}
        </div>
      ) : null}

      <section className="dashboard-table-card">
        <table aria-label="Журнал сообщений">
          <thead>
            <tr>
              <th>Платформа</th>
              <th>Автор</th>
              <th>Сообщение</th>
              <th>Диалог</th>
              <th>Тип</th>
              <th>Время</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>
                  <span className={`platform-chip platform-chip--${item.platform.toLowerCase()}`}>
                    {platformLabel(item.platform)}
                  </span>
                </td>
                <td>
                  <div className="table-meta">
                    <strong>{authorPrimary(item)}</strong>
                    {authorSecondary(item) ? <span>{authorSecondary(item)}</span> : null}
                  </div>
                </td>
                <td className="message-cell">{item.text || "Без текста"}</td>
                <td>
                  <div className="table-meta">
                    <strong>{conversationPrimary(item)}</strong>
                    {conversationSecondary(item) ? (
                      <span>{conversationSecondary(item)}</span>
                    ) : null}
                  </div>
                </td>
                <td>{item.messageType}</td>
                <td>{formatDateTime(item.sentAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {!isLoading && items.length === 0 && !error ? (
          <div className="dashboard-empty">Сообщения пока не найдены.</div>
        ) : null}

        {!error && items.length > 0 ? (
          <div className="dashboard-pagination">
            <span className="dashboard-pagination__status">Страница {page + 1}</span>
            <button
              type="button"
              className="dashboard-page-button"
              disabled={isLoading || page === 0}
              onClick={() => void refresh(search, platform, page - 1, pageSize)}
            >
              Назад
            </button>
            <button
              type="button"
              className="dashboard-page-button"
              disabled={isLoading || !hasNextPage}
              onClick={() => void refresh(search, platform, page + 1, pageSize)}
            >
              Вперёд
            </button>
          </div>
        ) : null}
      </section>
    </main>
  );
}
