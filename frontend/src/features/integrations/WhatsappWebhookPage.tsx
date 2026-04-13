type WhatsappWebhookPageProps = {
  token: string;
  onUnauthorized?: () => void;
};

const whatsappCapabilities = [
  {
    title: "Webhook-first ingestion",
    text: "Канал принимает входящие события Wappi через webhook и не зависит от polling-контура."
  },
  {
    title: "Медиа как metadata + content",
    text: "Если Wappi присылает binary payload inline, он может быть сохранён вместе с attachment metadata. Если приходит только URL, сохраняется трассируемая ссылка."
  },
  {
    title: "Изоляция от MAX",
    text: "WhatsApp остаётся в доменной модели как отдельный WAPPI-канал и не смешивается с новым MAX ingestion path."
  }
] as const;

const whatsappNotes = [
  "Основной ingest webhook принимает `incoming_message` и file events от Wappi.",
  "Вложения проходят через attachment metadata слой, а при наличии inline content попадают и в binary storage.",
  "Endpoint ориентирован на публичный webhook-delivery и operational monitoring, а не на ручное управление из UI."
] as const;

export function WhatsappWebhookPage(_: WhatsappWebhookPageProps) {
  return (
    <main className="integrations-shell whatsapp-page">
      <section className="integrations-hero whatsapp-page__hero">
        <div>
          <p className="integrations-hero__eyebrow">Платформа</p>
          <h1>WhatsApp</h1>
          <p>
            Операционный экран inbound webhook-канала WhatsApp через Wappi.
            Здесь зафиксирован endpoint приёма, ingest scope и текущая модель
            обработки медиа без ручных actions.
          </p>
        </div>

        <div className="whatsapp-page__signal" aria-label="Webhook status">
          <span className="whatsapp-page__signal-label">Channel mode</span>
          <strong>Webhook live</strong>
          <span>Inbound events via Wappi</span>
        </div>
      </section>

      <section className="whatsapp-page__layout">
        <article className="integration-panel integration-panel--whatsapp whatsapp-page__primary">
          <header className="integration-panel__header">
            <div>
              <p className="integration-panel__eyebrow">Ingress</p>
              <h2>Webhook</h2>
            </div>
            <span className="state-badge state-badge--whatsapp">WHATSAPP / WAPPI</span>
          </header>

          <div className="whatsapp-page__endpoint-card">
            <span>Endpoint</span>
            <code>/api/integrations/webhooks/wappi</code>
          </div>

          <div className="integration-metrics whatsapp-page__metrics">
            <div className="integration-metric">
              <span>Режим</span>
              <strong>Inbound webhook channel</strong>
            </div>
            <div className="integration-metric">
              <span>Основное событие</span>
              <strong>`incoming_message`</strong>
            </div>
            <div className="integration-metric">
              <span>Платформа</span>
              <strong>`Platform.WAPPI`</strong>
            </div>
            <div className="integration-metric">
              <span>Attachment path</span>
              <strong>Metadata + optional binary content</strong>
            </div>
          </div>

          <p className="integration-note">
            Этот экран отражает уже работающий inbound контур WhatsApp через
            Wappi. Основной сценарий здесь не управление, а проверка endpoint,
            ingest semantics и текущей operational модели вложений.
          </p>
        </article>

        <div className="whatsapp-page__secondary">
          <article className="integration-panel integration-panel--whatsapp-soft">
            <header className="integration-panel__header">
              <div>
                <p className="integration-panel__eyebrow">Capabilities</p>
                <h2>Что сейчас включено</h2>
              </div>
            </header>

            <div className="whatsapp-capability-list">
              {whatsappCapabilities.map((item) => (
                <article key={item.title} className="whatsapp-capability-card">
                  <strong>{item.title}</strong>
                  <p>{item.text}</p>
                </article>
              ))}
            </div>
          </article>

          <article className="integration-panel integration-panel--whatsapp-soft">
            <header className="integration-panel__header">
              <div>
                <p className="integration-panel__eyebrow">Operational Notes</p>
                <h2>Контур работы</h2>
              </div>
            </header>

            <ul className="whatsapp-note-list">
              {whatsappNotes.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </article>
        </div>
      </section>
    </main>
  );
}
