# WhatsApp Wappi Webhook Ingestion Design

## Goal

Run Wappi inbound WhatsApp ingestion through webhooks instead of polling, while keeping the existing normalized ingestion pipeline and storing media binaries in PostgreSQL.

## Deployment Constraint

The application server is private on `192.168.88.12`, so public webhook delivery is exposed through Tailscale Funnel:

- internal app: `http://127.0.0.1:18080`
- public HTTPS entrypoint: `https://debian.tail9e3c2c.ts.net`
- Wappi webhook URL: `https://debian.tail9e3c2c.ts.net/api/integrations/webhooks/wappi`

## Recommended Approach

Use Wappi webhook delivery as the only inbound source for WhatsApp messages in this service:

- Wappi sends `incoming_message` and file events to the Spring controller
- the controller maps each item into `InboundIntegrationEvent`
- the existing ingestion pipeline persists `IntegrationSource`, `Conversation`, `ExternalUser`, `Message`, `MessageAttachment`, and `RawEvent`
- binary media content is stored in `message_attachment_content`

## Runtime Design

1. Wappi POSTs JSON payloads with `messages[]` to `/api/integrations/webhooks/wappi`
2. `WappiWebhookController` iterates over `messages`
3. `WappiInboundEventMapper` creates normalized inbound events with stable IDs
4. `IntegrationIngestionService` persists raw event, message, conversation, author, and attachments
5. `MessagePersistenceService` writes attachment metadata and, when present, binary content into `message_attachment_content`

## Attachment Handling

Wappi media is stored in two layers:

- `message_attachment` keeps external attachment ID, URL, MIME type, and metadata JSON
- `message_attachment_content` stores decoded binary content, original file name, content size, and SHA-256

If Wappi sends only a remote media URL, the metadata row is still persisted and the attachment remains traceable without local binary content.

## Idempotency

Idempotency is enforced by existing normalized constraints:

- `raw_event(platform, event_id)` rejects replay of the same Wappi webhook event
- `message(conversation_id, external_message_id)` prevents duplicate messages inside a conversation

The mapper derives event identity from Wappi `profile_id` and message `id`.

## Tailscale Funnel

Funnel is enabled with:

```bash
sudo tailscale set --operator=deployer
tailscale funnel --bg --yes 18080
```

`--bg` keeps the Funnel configuration in Tailscale so it is restored after machine reboot, assuming the `tailscaled` service starts successfully.

## Wappi Admin Settings

Configure Wappi with:

- `Webhook url`: `https://debian.tail9e3c2c.ts.net/api/integrations/webhooks/wappi`
- enabled webhook type: `Все входящие сообщения и файлы`

The additional Wappi webhook types for sent-message status and profile authorization are optional and are not required for inbound message ingestion.
