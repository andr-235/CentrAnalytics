# MAX Wappi Webhook Ingestion Design

## Goal

Add inbound MAX ingestion through Wappi webhooks by reusing the existing normalized integration pipeline, while keeping the current WhatsApp Wappi flow isolated and unchanged.

## Scope

This change covers only inbound MAX messages delivered by Wappi webhook callbacks.

Included:

- inbound text and media messages from MAX
- webhook controller, DTOs, mapper, and tests
- persistence through the existing integration ingestion pipeline

Excluded:

- delivery status callbacks
- authorization status callbacks
- outbound sending through MAX
- downloading remote media binaries from MAX URLs

## Recommended Approach

Implement MAX as a separate inbound channel under the existing integration subsystem:

- add a dedicated webhook endpoint for MAX, separate from the WhatsApp Wappi endpoint
- add MAX-specific DTOs that match the Wappi MAX webhook payload
- add a MAX-specific mapper that converts webhook payloads into `InboundIntegrationEvent`
- ingest mapped events through the existing `IntegrationIngestionService`
- store MAX as its own platform in the domain model so analytics and source status do not mix with WhatsApp

This keeps the current WhatsApp code stable and avoids mixing two payload formats in the same mapper.

## Runtime Design

1. Wappi sends MAX webhook payloads to `/api/integrations/webhooks/wappi/max`.
2. The controller accepts the JSON payload and extracts incoming message entries.
3. For each incoming message, `MaxInboundEventMapper` builds a normalized `InboundIntegrationEvent`.
4. `IntegrationIngestionService` persists the raw event, integration source, conversation, external user, message, and attachment metadata.
5. The controller returns `{"status":"accepted","processed":N}` in the same style as the current WhatsApp webhook.

## Payload Mapping

The MAX mapper should normalize these concepts:

- platform: `Platform.MAX`
- event type: inbound webhook type from Wappi, defaulting to `incoming_message`
- event id: stable composite key built from Wappi profile id and message id
- integration source external id: Wappi MAX profile id
- conversation external id: MAX chat identifier
- author external id: sender identifier from the webhook payload
- message external id: MAX message id
- message timestamp: webhook timestamp or epoch field, whichever is present
- text: message body for text messages, caption/title where applicable for media
- attachments: metadata based on `type`, `file_link`, `mime_type`, `file_name`, `media_info`, and related fields

If the payload contains non-message webhook events or unsupported message types, they should be ignored rather than failing the whole request.

## Platform Separation

MAX must be introduced as a distinct `Platform` enum value instead of reusing `WAPPI`.

Reason:

- WhatsApp and MAX are separate messaging networks
- overview and platform metrics should not aggregate them together
- platform-specific status and enablement in the frontend remain unambiguous

## Attachment Handling

MAX attachments are stored in the same two-layer model already used by the ingestion pipeline:

- `message_attachment` stores external attachment id, URL, MIME type, file name, and metadata JSON
- `message_attachment_content` is written only when inline binary content is available

For this first iteration, MAX remote files referenced by URL are stored as metadata only. No media download worker is introduced.

## Idempotency

Existing normalized constraints continue to provide idempotency:

- `raw_event(platform, event_id)` prevents replay of the same webhook event
- `message(conversation_id, external_message_id)` prevents duplicate messages inside one conversation

The MAX mapper should build the event id as a stable combination of profile id and message id, using the same pattern as the existing Wappi webhook ingestion.

## Configuration

Add a dedicated configuration property for the MAX webhook path, defaulting to:

`/api/integrations/webhooks/wappi/max`

No outbound API credentials or polling schedule are required for this scope.

## Testing

Add coverage in two layers:

- mapper unit tests for text messages, media messages, unsupported event filtering, and stable identifiers
- controller integration tests proving webhook acceptance and persistence of raw events, messages, and attachment metadata

## Risks And Constraints

- Wappi MAX payload fields may differ slightly from WhatsApp naming, so DTOs must follow the MAX documentation exactly
- if Wappi sends new MAX message types later, they should degrade to `UNKNOWN` instead of breaking ingestion
- introducing `Platform.MAX` may require small downstream updates anywhere platform enums are rendered or filtered

## Implementation Boundary

Keep this change narrowly focused on inbound ingestion. Do not fold WhatsApp and MAX into a shared refactor unless implementation reveals duplicated code severe enough to block maintainability.
