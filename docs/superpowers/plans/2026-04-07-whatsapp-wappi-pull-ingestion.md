# WhatsApp Wappi Pull Ingestion Implementation Plan

> Superseded on 2026-04-07 by `2026-04-07-whatsapp-wappi-webhook-ingestion.md`. Keep this file only as historical context for the earlier no-webhook option.

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a pull-based WhatsApp integration through Wappi that ingests only new inbound messages from the time the integration is enabled and stores media binaries in PostgreSQL.

**Architecture:** Extend the existing `integration` module with a Wappi-specific scheduled polling channel that reads `sync/messages/all`, filters only new inbound messages, maps them into `InboundIntegrationEvent`, and persists them through the current normalized ingestion pipeline. Add a dedicated persistence layer for binary attachment content linked to `message_attachment`.

**Tech Stack:** Spring Boot, Spring Scheduling, Spring Data JPA, Flyway, Jackson, JUnit 5, MockMvc, PostgreSQL

---

## Chunk 1: Persistence Foundation

### Task 1: Add binary attachment storage schema

**Files:**
- Create: `src/main/resources/db/migration/V7__create_message_attachment_content.sql`
- Modify: `src/test/java/com/ca/centranalytics/integration/domain/IntegrationPersistenceTest.java`

- [ ] Review existing attachment tables in `src/main/resources/db/migration/V2__create_integration_tables.sql`
- [ ] Add `message_attachment_content` with a unique FK to `message_attachment`, `content bytea`, `file_name`, `content_size`, `content_sha256`, and timestamps
- [ ] Extend persistence tests to verify one-to-one attachment content persistence and deduplication constraints
- [ ] Run: `./mvnw -Dtest=IntegrationPersistenceTest test`

### Task 2: Add JPA entity and repository for attachment bodies

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/domain/entity/MessageAttachmentContent.java`
- Create: `src/main/java/com/ca/centranalytics/integration/domain/repository/MessageAttachmentContentRepository.java`
- Test: `src/test/java/com/ca/centranalytics/integration/domain/IntegrationPersistenceTest.java`

- [ ] Write a failing persistence test for saving attachment metadata plus binary content
- [ ] Add the entity with a one-to-one link to `MessageAttachment`
- [ ] Add a repository with lookup by attachment ID
- [ ] Run: `./mvnw -Dtest=IntegrationPersistenceTest test`

## Chunk 2: Wappi Configuration And Contracts

### Task 3: Add Wappi configuration properties and platform support

**Files:**
- Modify: `src/main/java/com/ca/centranalytics/integration/domain/entity/Platform.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/config/WappiProperties.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/config/IntegrationProperties.java`
- Modify: `src/main/resources/application.properties`
- Modify: `.env.example`
- Test: `src/test/java/com/ca/centranalytics/integration/config/IntegrationPropertiesTest.java`

- [ ] Add `WHATSAPP` platform support
- [ ] Add Wappi properties for `enabled`, `baseUrl`, `token`, `profileId`, `pollInterval`, and `batchSize`
- [ ] Bind and document the new settings in application properties and `.env.example`
- [ ] Add configuration binding tests for the new properties
- [ ] Run: `./mvnw -Dtest=IntegrationPropertiesTest test`

### Task 4: Add Wappi transport DTOs and HTTP client abstraction

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/client/WappiClient.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/client/HttpWappiClient.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/client/dto/WappiMessagesResponse.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/client/dto/WappiMessageDto.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/client/dto/WappiAttachmentDto.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/client/dto/WappiChatDto.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/HttpWappiClientTest.java`

- [ ] Write a failing client test for parsing a representative `sync/messages/all` payload
- [ ] Add DTOs only for the fields needed in the first version
- [ ] Add a client interface so polling and mapping code do not depend on transport details
- [ ] Implement the HTTP client with token-based authentication and explicit parse errors
- [ ] Run: `./mvnw -Dtest=HttpWappiClientTest test`

## Chunk 3: Mapping And Binary Attachment Processing

### Task 5: Add Wappi inbound mapper for messages and attachments

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/service/WappiInboundEventMapper.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/WappiInboundEventMapperTest.java`

- [ ] Write failing tests for text, image, video, document, and audio inbound messages
- [ ] Map Wappi chats to `InboundConversation`
- [ ] Map Wappi senders to `InboundAuthor`
- [ ] Map Wappi messages to stable `InboundIntegrationEvent` and `InboundMessage` identities
- [ ] Preserve the original Wappi payload JSON in the mapped event
- [ ] Run: `./mvnw -Dtest=WappiInboundEventMapperTest test`

### Task 6: Add attachment binary decoding and hashing service

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/service/WappiAttachmentContentService.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/WappiAttachmentContentServiceTest.java`

- [ ] Write failing tests for base64 decoding, file-size calculation, and SHA-256 hashing
- [ ] Implement a focused service that converts Wappi attachment DTOs into binary persistence objects
- [ ] Fail explicitly when Wappi declares binary content but the payload is malformed
- [ ] Run: `./mvnw -Dtest=WappiAttachmentContentServiceTest test`

## Chunk 4: Checkpointing And Poll Filtering

### Task 7: Add checkpoint model for Wappi polling

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/service/WappiCheckpointService.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/domain/repository/IngestionCheckpointRepository.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/WappiCheckpointServiceTest.java`

- [ ] Write failing tests for first-run checkpoint initialization and ordered checkpoint advancement
- [ ] Add repository methods needed to fetch and upsert a Wappi-specific checkpoint
- [ ] Store checkpoint values as compact JSON with message time and external message ID
- [ ] Run: `./mvnw -Dtest=WappiCheckpointServiceTest test`

### Task 8: Add Wappi message filter for inbound-only and new-only rules

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/service/WappiMessageFilter.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/WappiMessageFilterTest.java`

- [ ] Write failing tests for:
- [ ] only inbound messages are accepted
- [ ] messages older than `integrationStartedAt` are rejected
- [ ] checkpoint boundary messages are handled deterministically by time and ID
- [ ] Implement filtering and stable sorting rules
- [ ] Run: `./mvnw -Dtest=WappiMessageFilterTest test`

## Chunk 5: Poller Integration With Existing Ingestion Pipeline

### Task 9: Add source bootstrap and Wappi polling orchestration

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/service/WappiSourceService.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/service/WappiPoller.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/WappiPollerTest.java`

- [ ] Write failing tests for first run without backfill and for successful batch processing
- [ ] Create or refresh the Wappi `IntegrationSource` with `integrationStartedAt` and profile settings
- [ ] Implement a scheduled poller that loads messages, applies filtering, ingests accepted events, and advances the checkpoint only through consecutive successes
- [ ] Run: `./mvnw -Dtest=WappiPollerTest test`

### Task 10: Extend message persistence to store attachment content

**Files:**
- Modify: `src/main/java/com/ca/centranalytics/integration/ingestion/dto/InboundAttachment.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/ingestion/service/MessagePersistenceService.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/ingestion/service/IntegrationIngestionService.java`
- Test: `src/test/java/com/ca/centranalytics/integration/ingestion/IntegrationIngestionServiceTest.java`

- [ ] Write a failing integration test that ingests a message with media and asserts both attachment metadata and attachment binary rows
- [ ] Extend `InboundAttachment` to carry optional attachment content fields needed by Wappi
- [ ] Persist `MessageAttachmentContent` alongside `MessageAttachment`
- [ ] Keep existing Telegram and VK attachment flows working when no binary payload is present
- [ ] Run: `./mvnw -Dtest=IntegrationIngestionServiceTest test`

## Chunk 6: Verification, API Visibility, And Documentation

### Task 11: Add end-to-end Wappi ingestion integration test

**Files:**
- Create: `src/test/java/com/ca/centranalytics/integration/channel/whatsapp/wappi/WappiIngestionIntegrationTest.java`

- [ ] Add a Spring-backed integration test covering:
- [ ] first checkpoint initialization
- [ ] next-cycle inbound message ingestion
- [ ] attachment metadata persistence
- [ ] binary content persistence
- [ ] duplicate polling safety
- [ ] Run: `./mvnw -Dtest=WappiIngestionIntegrationTest test`

### Task 12: Document Wappi configuration and operator flow

**Files:**
- Modify: `README.md`
- Modify: `.env.example`
- Create: `docs/superpowers/specs/2026-04-07-whatsapp-wappi-pull-ingestion-design.md`
- Create: `docs/superpowers/plans/2026-04-07-whatsapp-wappi-pull-ingestion.md`

- [ ] Document required Wappi settings and the “from activation onward” ingestion rule
- [ ] Document that the integration is polling-based and does not require a public webhook endpoint
- [ ] Save the approved design and implementation plan in the repository
- [ ] Run: `./mvnw test`

Plan complete and saved to `docs/superpowers/plans/2026-04-07-whatsapp-wappi-pull-ingestion.md`. Ready to execute?
