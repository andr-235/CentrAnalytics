import { useEffect, useState } from "react";

import { fetchMessages } from "./dashboard.api";
import type { MessageRecord } from "./dashboard.types";

type DashboardPageProps = {
  token: string;
  loadMessages?: typeof fetchMessages;
};

const platformOptions = ["ALL", "TELEGRAM", "WHATSAPP", "VK"];

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

export function DashboardPage({
  token,
  loadMessages = fetchMessages
}: DashboardPageProps) {
  const [items, setItems] = useState<MessageRecord[]>([]);
  const [search, setSearch] = useState("");
  const [platform, setPlatform] = useState("ALL");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  async function refresh(nextSearch = search, nextPlatform = platform) {
    setIsLoading(true);
    setError("");

    const result = await loadMessages(token, {
      search: nextSearch,
      platform: nextPlatform
    });

    if (!result.ok) {
      setItems([]);
      setError(result.error);
      setIsLoading(false);
      return;
    }

    setItems(result.items);
    setIsLoading(false);
  }

  useEffect(() => {
    void refresh("", "ALL");
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

          <button
            type="button"
            className="dashboard-refresh"
            onClick={() => void refresh()}
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
                <td>{item.authorId ?? "System"}</td>
                <td className="message-cell">{item.text || "Без текста"}</td>
                <td>{item.conversationId ?? "—"}</td>
                <td>{item.messageType}</td>
                <td>{formatDateTime(item.sentAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {!isLoading && items.length === 0 && !error ? (
          <div className="dashboard-empty">Сообщения пока не найдены.</div>
        ) : null}
      </section>
    </main>
  );
}
