# MAX Wappi Webhook Ingestion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add inbound MAX webhook ingestion through Wappi without changing the existing WhatsApp ingestion behavior.

**Architecture:** Introduce a MAX-specific webhook controller, DTOs, and mapper that normalize Wappi MAX payloads into the existing ingestion pipeline. Keep MAX isolated as its own platform enum value so persistence and analytics stay distinct from WhatsApp.

**Tech Stack:** Java 26, Spring Boot, Jackson, JUnit 5, MockMvc, Testcontainers, Maven Wrapper

---

### Task 1: Add Platform And Config Support

**Files:**
- Modify: `src/main/java/com/ca/centranalytics/integration/domain/entity/Platform.java`
- Modify: `src/main/resources/application.properties`
- Test: `src/test/java/com/ca/centranalytics/integration/config/IntegrationPropertiesTest.java`

- [ ] **Step 1: Write the failing configuration/platform test**

```java
@Test
void shouldExposeMaxWebhookPathProperty() {
    assertThat(environment.getProperty("integration.max.webhook-path"))
            .isEqualTo("/api/integrations/webhooks/wappi/max");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=IntegrationPropertiesTest test`
Expected: FAIL because `integration.max.webhook-path` is not configured yet.

- [ ] **Step 3: Add the platform enum and property**

```java
public enum Platform {
    VK,
    TELEGRAM,
    WAPPI,
    MAX,
    ODNOKLASSNIKI
}
```

```properties
integration.max.webhook-path=${MAX_WEBHOOK_PATH:/api/integrations/webhooks/wappi/max}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=IntegrationPropertiesTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ca/centranalytics/integration/domain/entity/Platform.java src/main/resources/application.properties src/test/java/com/ca/centranalytics/integration/config/IntegrationPropertiesTest.java
git commit -m "feat: add max webhook configuration"
```

### Task 2: Add MAX Webhook DTOs And Mapper

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/max/wappi/client/dto/MaxWebhookPayload.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/max/wappi/client/dto/MaxMessageDto.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/max/wappi/service/MaxInboundEventMapper.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/max/wappi/MaxInboundEventMapperTest.java`

- [ ] **Step 1: Write the failing mapper tests**

```java
@Test
void mapsIncomingTextMessage() {
    MaxMessageDto message = new MaxMessageDto(
            "incoming_message",
            "profile-1",
            "msg-1",
            "text",
            "Привет",
            "user-1",
            "chat-1",
            "Sender",
            "2026-04-13T10:15:30+03:00",
            null,
            null,
            null,
            null,
            null
    );

    InboundIntegrationEvent event = mapper.map(message);

    assertThat(event.platform()).isEqualTo(Platform.MAX);
    assertThat(event.eventId()).isEqualTo("max-profile-1-msg-1");
    assertThat(event.message().text()).isEqualTo("Привет");
    assertThat(event.message().attachments()).isEmpty();
}

@Test
void mapsIncomingDocumentMessageWithAttachmentMetadata() {
    MaxMessageDto message = new MaxMessageDto(
            "incoming_message",
            "profile-1",
            "msg-2",
            "document",
            "https://files.example/test.pdf",
            "user-2",
            "chat-2",
            "Sender",
            null,
            1776064530L,
            "Документ",
            "https://files.example/test.pdf",
            "application/pdf",
            "test.pdf",
            "{\"size\":12345}"
    );

    InboundIntegrationEvent event = mapper.map(message);

    assertThat(event.message().messageType()).isEqualTo(MessageType.DOCUMENT);
    assertThat(event.message().attachments()).singleElement()
            .satisfies(attachment -> {
                assertThat(attachment.url()).isEqualTo("https://files.example/test.pdf");
                assertThat(attachment.mimeType()).isEqualTo("application/pdf");
                assertThat(attachment.fileName()).isEqualTo("test.pdf");
            });
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=MaxInboundEventMapperTest test`
Expected: FAIL because MAX DTOs and mapper do not exist.

- [ ] **Step 3: Write minimal DTOs and mapper**

```java
public record MaxWebhookPayload(
        List<MaxMessageDto> messages
) {
}
```

```java
public record MaxMessageDto(
        @JsonProperty("wh_type") String whType,
        @JsonProperty("profile_id") String profileId,
        String id,
        String type,
        String body,
        String from,
        @JsonProperty("chat_id") String chatId,
        @JsonProperty("sender_name") String senderName,
        String timestamp,
        Long time,
        String caption,
        @JsonProperty("file_link") String fileLink,
        @JsonProperty("mime_type") String mimeType,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("media_info") String mediaInfo
) {
}
```

```java
public InboundIntegrationEvent map(MaxMessageDto message) {
    return new InboundIntegrationEvent(
            Platform.MAX,
            StringUtils.hasText(message.whType()) ? message.whType() : "incoming_message",
            "max-" + message.profileId() + "-" + message.id(),
            writeValue(message),
            true,
            message.profileId(),
            "MAX profile " + message.profileId(),
            writeValue(Map.of("profileId", message.profileId())),
            new InboundConversation(message.chatId(), ConversationType.DIRECT, message.chatId(), null),
            new InboundAuthor(message.from(), message.senderName(), null, null, null, null, null, false, null),
            new InboundMessage(
                    message.id(),
                    parseInstant(message),
                    resolveText(message),
                    lower(resolveText(message)),
                    mapMessageType(message.type()),
                    null,
                    null,
                    extractAttachments(message)
            )
    );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=MaxInboundEventMapperTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ca/centranalytics/integration/channel/max/wappi/client/dto/MaxWebhookPayload.java src/main/java/com/ca/centranalytics/integration/channel/max/wappi/client/dto/MaxMessageDto.java src/main/java/com/ca/centranalytics/integration/channel/max/wappi/service/MaxInboundEventMapper.java src/test/java/com/ca/centranalytics/integration/channel/max/wappi/MaxInboundEventMapperTest.java
git commit -m "feat: map max wappi inbound events"
```

### Task 3: Add MAX Webhook Controller And Persistence Test

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/max/wappi/controller/MaxWebhookController.java`
- Create: `src/test/java/com/ca/centranalytics/integration/channel/max/wappi/MaxWebhookControllerTest.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/max/wappi/MaxWebhookControllerTest.java`

- [ ] **Step 1: Write the failing controller integration test**

```java
@Test
void acceptsIncomingMaxWebhookAndPersistsMessage() throws Exception {
    mockMvc.perform(post("/api/integrations/webhooks/wappi/max")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "messages": [
                                {
                                  "wh_type": "incoming_message",
                                  "profile_id": "profile-1",
                                  "id": "msg-1",
                                  "type": "text",
                                  "body": "Привет из MAX",
                                  "from": "user-1",
                                  "chat_id": "chat-1",
                                  "sender_name": "Maks",
                                  "timestamp": "2026-04-13T10:15:30+03:00"
                                }
                              ]
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("accepted"))
            .andExpect(jsonPath("$.processed").value(1));

    assertThat(rawEventRepository.findAll()).hasSize(1);
    assertThat(messageRepository.findAll()).hasSize(1);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=MaxWebhookControllerTest test`
Expected: FAIL because the MAX controller endpoint does not exist.

- [ ] **Step 3: Add the controller**

```java
@RestController
@RequiredArgsConstructor
public class MaxWebhookController {

    private final MaxInboundEventMapper maxInboundEventMapper;
    private final IntegrationIngestionService integrationIngestionService;

    @PostMapping(
            path = "${integration.max.webhook-path:/api/integrations/webhooks/wappi/max}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> handleWebhook(@RequestBody MaxWebhookPayload payload) {
        List<MaxMessageDto> messages = payload.messages() == null ? List.of() : payload.messages();
        messages.stream()
                .filter(message -> "incoming_message".equals(message.whType()) || !StringUtils.hasText(message.whType()))
                .map(maxInboundEventMapper::map)
                .forEach(integrationIngestionService::ingest);
        return Map.of("status", "accepted", "processed", messages.size());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=MaxWebhookControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ca/centranalytics/integration/channel/max/wappi/controller/MaxWebhookController.java src/test/java/com/ca/centranalytics/integration/channel/max/wappi/MaxWebhookControllerTest.java
git commit -m "feat: ingest max wappi webhooks"
```

### Task 4: Run Focused Verification

**Files:**
- Test: `src/test/java/com/ca/centranalytics/integration/config/IntegrationPropertiesTest.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/max/wappi/MaxInboundEventMapperTest.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/max/wappi/MaxWebhookControllerTest.java`

- [ ] **Step 1: Run focused verification**

Run: `./mvnw -Dtest=IntegrationPropertiesTest,MaxInboundEventMapperTest,MaxWebhookControllerTest test`
Expected: PASS

- [ ] **Step 2: Run broader regression check for existing Wappi webhook**

Run: `./mvnw -Dtest=WappiWebhookControllerTest test`
Expected: PASS

- [ ] **Step 3: Commit verification-safe result**

```bash
git add .
git commit -m "test: cover max wappi webhook ingestion"
```
