# WhatsApp Wappi Pull Ingestion Design

> Superseded on 2026-04-07 by `2026-04-07-whatsapp-wappi-webhook-ingestion-design.md`. Keep this file only as historical context for the earlier no-webhook option.

## Goal

Add a WhatsApp integration through Wappi that:

1. Polls Wappi without using webhooks
2. Starts ingesting only messages received after the integration is enabled
3. Persists all incoming messages through the existing normalized ingestion pipeline
4. Stores media binaries in PostgreSQL instead of only keeping attachment metadata

## Recommended Approach

Use a pull-based Wappi channel built on top of the current `integration` ingestion model:

- Wappi `sync/messages/all` is the primary source of new messages
- The integration records its own activation time and never backfills older history
- New Wappi messages are mapped into the existing `InboundIntegrationEvent` contract
- Existing ingestion services continue to persist `IntegrationSource`, `Conversation`, `ExternalUser`, `Message`, `MessageAttachment`, and `RawEvent`
- Media bytes are stored in a new attachment-content table linked to `message_attachment`

This keeps the current ingestion architecture intact and avoids introducing a second persistence path for WhatsApp.

## Runtime Design

The Wappi integration is implemented as a scheduled polling flow:

1. Load Wappi source settings and ingestion checkpoint
2. If no checkpoint exists yet, initialize it at the integration start time and stop
3. Call Wappi `sync/messages/all`
4. Sort messages deterministically by message timestamp and external message ID
5. Filter out:
   - outbound messages
   - messages older than `integrationStartedAt`
   - messages already behind the last successful checkpoint
6. Map each accepted Wappi message into `InboundIntegrationEvent`
7. Persist the normalized message through `IntegrationIngestionService`
8. Persist media bytes for each attachment in a dedicated binary table
9. Advance the checkpoint only through the last consecutively successful message

The system must not skip over a failed message when moving the checkpoint. A failed message remains retryable on the next poll.

## Data Model

### Existing normalized entities reused

- Wappi profile -> `IntegrationSource`
- Wappi chat -> `Conversation`
- Wappi contact -> `ExternalUser`
- Wappi message -> `Message`
- Wappi attachment metadata -> `MessageAttachment`
- Raw Wappi payload -> `RawEvent`

### New persistence

Add a new table for attachment bodies, for example `message_attachment_content`, with:

- `id`
- `attachment_id` unique FK to `message_attachment`
- `content bytea not null`
- `file_name`
- `content_size`
- `content_sha256`
- `created_at`

`message_attachment` remains the metadata record. The new table stores the binary payload.

## Source Settings And Checkpoints

Wappi-specific source settings should be stored in `integration_source.settings_json` and include:

- `profileId`
- `apiBaseUrl`
- `integrationStartedAt`
- non-secret descriptive settings needed for diagnostics

The access token must stay in application configuration, not in the database.

Use `ingestion_checkpoint` with a Wappi-specific checkpoint type such as `WAPPI_MESSAGES_SYNC`. The checkpoint value should be a compact JSON string containing:

- last processed message time
- last processed external message ID

This gives a stable tie-breaker when multiple messages share the same timestamp.

## Idempotency

Idempotency is enforced in two layers:

1. `raw_event(platform, event_id)` prevents replay of the same mapped Wappi event
2. `message(conversation_id, external_message_id)` prevents duplicate messages inside a conversation

The mapper must produce stable event IDs derived from Wappi message identity so repeated polling of overlapping windows remains safe.

## Attachment Handling

The integration must persist all media exposed by Wappi for inbound messages. For each attachment:

- create a `MessageAttachment` with type, MIME type, external attachment ID, URL if present, and raw metadata
- decode the binary payload returned by Wappi
- store it in `message_attachment_content.content`
- populate file name, byte size, and SHA-256 hash

If Wappi returns attachment metadata without an actual payload, the metadata should still be stored and the missing binary state should be traceable through metadata or logs.

## Error Handling

- HTTP or deserialization failures should mark the checkpoint as failed and stop the poll cycle
- A single message persistence failure should stop checkpoint advancement after the last successful message
- Raw Wappi payloads should always be captured before normalization is considered successful
- Binary decode errors should fail the message ingestion rather than silently dropping media

## Testing Strategy

Add the following test layers:

- Unit tests for Wappi payload mapping into `InboundIntegrationEvent`
- Unit tests for inbound-only filtering and checkpoint boundary handling
- Unit tests for binary attachment decoding and hashing
- Integration tests for persisting message, attachment metadata, and binary content together
- Deduplication tests for repeated polling of the same message set
- First-run tests proving that no historical backlog is ingested before the first checkpoint is created

Real Wappi network calls are out of scope for automated tests in the first version.

## Why This Design

- Avoids the webhook requirement, which does not fit the current private server deployment on `192.168.88.12`
- Reuses the existing normalized ingestion architecture instead of bypassing it
- Enforces a clean “from activation onward” contract with no accidental history backfill
- Stores media in PostgreSQL in a form that is queryable and durable
- Keeps polling idempotent and safe across overlapping windows and retries
