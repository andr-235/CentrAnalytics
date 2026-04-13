type MaxWebhookPageProps = {
  token: string;
  onUnauthorized?: () => void;
};

const maxCapabilities = [
  {
    title: "Только входящий поток",
    text: "Канал принимает только inbound webhook-события из Wappi. Исходящие действия и статусы доставки в этот контур не входят."
  },
  {
    title: "Нормализация в ingestion",
    text: "MAX-события переводятся в общую модель сообщений, диалогов, авторов и source status без смешивания с WhatsApp."
  },
  {
    title: "Вложения без скачивания",
    text: "На первом этапе сохраняются metadata, MIME type и remote URL. Бинарный download worker не подключён."
  }
] as const;

const maxNotes = [
  "Поддерживаются входящие text, image, video, document, audio, contact и location события.",
  "Webhook-тип `incoming_message` ingest-ится, системные и status callbacks игнорируются.",
  "Платформа ведётся отдельно как `MAX`, поэтому в аналитике не смешивается с WAPPI / WhatsApp."
] as const;

export function MaxWebhookPage(_: MaxWebhookPageProps) {
  return (
    <main className="integrations-shell max-page">
      <section className="integrations-hero max-page__hero">
        <div>
          <p className="integrations-hero__eyebrow">Платформа</p>
          <h1>Max</h1>
          <p>
            Read-only webhook ingress для входящих сообщений MAX через Wappi.
            Страница фиксирует operational state канала, endpoint и текущую
            ingest-конфигурацию без управляющих действий.
          </p>
        </div>

        <div className="max-page__signal" aria-label="Webhook status">
          <span className="max-page__signal-label">Channel mode</span>
          <strong>Inbound only</strong>
          <span>Webhook active path reserved</span>
        </div>
      </section>

      <section className="max-page__layout">
        <article className="integration-panel integration-panel--max max-page__primary">
          <header className="integration-panel__header">
            <div>
              <p className="integration-panel__eyebrow">Ingress</p>
              <h2>Webhook</h2>
            </div>
            <span className="state-badge state-badge--max">MAX / WAPPI</span>
          </header>

          <div className="max-page__endpoint-card">
            <span>Endpoint</span>
            <code>/api/integrations/webhooks/wappi/max</code>
          </div>

          <div className="integration-metrics max-page__metrics">
            <div className="integration-metric">
              <span>Режим</span>
              <strong>Read-only webhook ingress</strong>
            </div>
            <div className="integration-metric">
              <span>Событие</span>
              <strong>`incoming_message`</strong>
            </div>
            <div className="integration-metric">
              <span>Платформа</span>
              <strong>`Platform.MAX`</strong>
            </div>
            <div className="integration-metric">
              <span>Схема хранения</span>
              <strong>Raw event + normalized entities</strong>
            </div>
          </div>

          <p className="integration-note">
            Канал предназначен для приёма входящих событий от Wappi и прокладки их
            в общий ingestion pipeline. Любые delivery/auth callbacks сознательно
            оставлены за пределами этой страницы и backend scope.
          </p>
        </article>

        <div className="max-page__secondary">
          <article className="integration-panel integration-panel--max-soft">
            <header className="integration-panel__header">
              <div>
                <p className="integration-panel__eyebrow">Capabilities</p>
                <h2>Что сейчас включено</h2>
              </div>
            </header>

            <div className="max-capability-list">
              {maxCapabilities.map((item) => (
                <article key={item.title} className="max-capability-card">
                  <strong>{item.title}</strong>
                  <p>{item.text}</p>
                </article>
              ))}
            </div>
          </article>

          <article className="integration-panel integration-panel--max-soft">
            <header className="integration-panel__header">
              <div>
                <p className="integration-panel__eyebrow">Operational Notes</p>
                <h2>Контур работы</h2>
              </div>
            </header>

            <ul className="max-note-list">
              {maxNotes.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </article>
        </div>
      </section>
    </main>
  );
}
