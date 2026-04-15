# VK Official Only Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove all VK fallback parsing and hybrid-mode behavior so VK integration runs only through the official VK API/SDK while preserving working official endpoints and jobs.

**Architecture:** Keep `VkOfficialClient` as the only integration boundary for VK. Remove `collectionMode` switching, fallback beans, fallback parsing helpers, and all service/orchestrator branches that call unofficial paths; then update tests and config binding to match the reduced contract.

**Tech Stack:** Spring Boot, Java 26, JUnit 5, MockMvc, Maven Wrapper, VK Java SDK.

---

## File Map

- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/SearchVkGroupsRequest.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/SearchVkUsersRequest.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/CollectVkGroupPostsRequest.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/CollectVkPostCommentsRequest.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/EnrichVkUsersRequest.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/VkGroupCollectRequest.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/config/VkProperties.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/config/VkAutoCollectionProperties.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/HttpVkOfficialClient.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/domain/VkCollectionMethod.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkDiscoveryOrchestrator.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkGroupManagementService.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkAutoCollectionService.java`
- Modify: `src/main/resources/application.properties`
- Delete: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/HttpVkFallbackClient.java`
- Delete: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/VkFallbackClient.java`
- Delete: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/JsJsonExtractor.java`
- Delete: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/JsCommentStripper.java`
- Delete: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/NoopVkFallbackClient.java`
- Delete: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkFallbackPolicy.java`
- Modify/Delete tests under `src/test/java/com/ca/centranalytics/integration/channel/vk/`
- Modify: `src/test/java/com/ca/centranalytics/integration/config/IntegrationPropertiesTest.java`
- Modify: `src/test/java/com/ca/centranalytics/integration/api/IntegrationApiTest.java`
- Modify: `src/test/java/com/ca/centranalytics/integration/domain/IntegrationPersistenceTest.java`

### Task 1: Lock Official-Only API Contract With Failing Tests

**Files:**
- Modify: `src/test/java/com/ca/centranalytics/integration/config/IntegrationPropertiesTest.java`
- Modify: `src/test/java/com/ca/centranalytics/integration/api/IntegrationApiTest.java`
- Modify: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkGroupManagementServiceTest.java`

- [ ] **Step 1: Write failing tests for removed config and request fields**

```java
assertThat(vkProperties.accessToken()).isEqualTo("vk-token");
assertThat(vkProperties.apiBaseUrl()).isEqualTo("https://api.vk.com/method");
assertThat(vkAutoCollectionProperties.fixedDelayMs()).isEqualTo(900000L);
```

```java
mockMvc.perform(post("/api/admin/integrations/vk/groups/search")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {"region":"Primorsky Krai","limit":25}
                """))
    .andExpect(status().isAccepted());
```

- [ ] **Step 2: Run targeted tests to verify they fail on current hybrid contract**

Run: `./mvnw -Dtest=IntegrationPropertiesTest,IntegrationApiTest,VkGroupManagementServiceTest test`
Expected: FAIL on missing/changed assertions or request payload mismatch.

- [ ] **Step 3: Update request payload expectations to official-only**

```java
var response = fixture.service.collectGroups(List.of("1001", "primorye_group", "missing_group"), 10, 5, 20);
assertThat(fixture.commandService.groupPostCalls())
        .containsExactly(new GroupPostCall(1001L, new CollectVkGroupPostsRequest(10)));
```

- [ ] **Step 4: Run targeted tests again to keep them red for the right reason**

Run: `./mvnw -Dtest=IntegrationPropertiesTest,IntegrationApiTest,VkGroupManagementServiceTest test`
Expected: FAIL in production code paths still expecting `collectionMode`/fallback config.

### Task 2: Remove Hybrid/Fallback From Request DTOs and Config Binding

**Files:**
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/SearchVkGroupsRequest.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/SearchVkUsersRequest.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/CollectVkGroupPostsRequest.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/CollectVkPostCommentsRequest.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/EnrichVkUsersRequest.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/VkGroupCollectRequest.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/config/VkProperties.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/config/VkAutoCollectionProperties.java`
- Modify: `src/main/resources/application.properties`

- [ ] **Step 1: Write the minimal production changes**

```java
public record SearchVkGroupsRequest(
        @NotBlank(message = "region is required") String region,
        @Min(value = 1, message = "limit must be greater than 0")
        @Max(value = 1000, message = "limit must be less than or equal to 1000") Integer limit
) {
}
```

```java
public record VkProperties(
        long groupId,
        String accessToken,
        String apiVersion,
        String apiBaseUrl,
        Duration requestTimeout
) {
}
```

```properties
integration.vk.group-id=${VK_GROUP_ID:0}
integration.vk.access-token=${VK_ACCESS_TOKEN:}
integration.vk.api-version=${VK_API_VERSION:5.199}
integration.vk.api-base-url=${VK_API_BASE_URL:https://api.vk.com/method}
integration.vk.request-timeout=${VK_REQUEST_TIMEOUT:5s}
```

- [ ] **Step 2: Remove `collectionMode` plumbing from group management request helpers**

```java
public record VkGroupCollectRequest(
        @NotEmpty(message = "groupIdentifiers must not be empty") List<String> groupIdentifiers,
        Integer postLimit,
        Integer commentPostLimit,
        Integer commentLimit
) {
}
```

- [ ] **Step 3: Run the contract tests**

Run: `./mvnw -Dtest=IntegrationPropertiesTest,IntegrationApiTest,VkGroupManagementServiceTest test`
Expected: PASS for DTO/config contract changes, remaining failures only in fallback-dependent services.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/ca/centranalytics/integration/channel/vk/api src/main/java/com/ca/centranalytics/integration/channel/vk/config src/main/resources/application.properties src/test/java/com/ca/centranalytics/integration/config/IntegrationPropertiesTest.java src/test/java/com/ca/centranalytics/integration/api/IntegrationApiTest.java src/test/java/com/ca/centranalytics/integration/channel/vk/VkGroupManagementServiceTest.java
git commit -m "refactor: remove vk hybrid request contract"
```

### Task 3: Refactor VK Services To Official Client Only

**Files:**
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkDiscoveryOrchestrator.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkGroupManagementService.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkAutoCollectionService.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/HttpVkOfficialClient.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/domain/VkCollectionMethod.java`

- [ ] **Step 1: Write failing service tests for official-only execution path**

```java
verify(vkOfficialClient).searchGroups(anyString(), eq(10));
verifyNoMoreInteractions(vkOfficialClient);
assertThat(savedCandidate.getCollectionMethod()).isEqualTo(VkCollectionMethod.OFFICIAL_API);
```

- [ ] **Step 2: Run VK service tests to verify red state**

Run: `./mvnw -Dtest=VkDiscoveryOrchestratorTest,VkDiscoveryOrchestratorSearchTest,VkAutoCollectionServiceTest,VkAutoCollectionSchedulerTest test`
Expected: FAIL because constructors and service logic still require fallback collaborators and `FALLBACK` enum branches.

- [ ] **Step 3: Implement official-only service logic**

```java
List<GroupSearchHit> results = searchGroups(searchTerms, request.limit());
VkCollectionMethod method = VkCollectionMethod.OFFICIAL_API;
```

```java
results = vkOfficialClient.getGroupPosts(resolveGroupDomain(groupId), request.limit());
Map<Long, VkUserCandidate> authors = enrichUsers(extractAuthorIds(...));
```

```java
private String userAccessToken() {
    return officialAccessToken();
}
```

```java
public enum VkCollectionMethod {
    OFFICIAL_API
}
```

- [ ] **Step 4: Run VK service tests again**

Run: `./mvnw -Dtest=VkDiscoveryOrchestratorTest,VkDiscoveryOrchestratorSearchTest,VkAutoCollectionServiceTest,VkAutoCollectionSchedulerTest,HttpVkOfficialClientTest test`
Expected: PASS for official-only services and client.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ca/centranalytics/integration/channel/vk/service src/main/java/com/ca/centranalytics/integration/channel/vk/client/HttpVkOfficialClient.java src/main/java/com/ca/centranalytics/integration/channel/vk/domain/VkCollectionMethod.java src/test/java/com/ca/centranalytics/integration/channel/vk
git commit -m "refactor: make vk discovery official only"
```

### Task 4: Delete Fallback Classes and Remove Residual Fallback Assertions

**Files:**
- Delete: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/HttpVkFallbackClient.java`
- Delete: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/VkFallbackClient.java`
- Delete: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/JsJsonExtractor.java`
- Delete: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/JsCommentStripper.java`
- Delete: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/NoopVkFallbackClient.java`
- Delete: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkFallbackPolicy.java`
- Delete/Modify: `src/test/java/com/ca/centranalytics/integration/channel/vk/HttpVkFallbackClientTest.java`
- Delete/Modify: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkFallbackPolicyTest.java`
- Modify: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkOfficialMapperTest.java`
- Modify: `src/test/java/com/ca/centranalytics/integration/domain/IntegrationPersistenceTest.java`

- [ ] **Step 1: Delete dead fallback code and rewrite residual enum-based tests**

```java
var snapshot = commentSnapshotMapper.map(result, VkCollectionMethod.OFFICIAL_API);
assertThat(snapshot.getCollectionMethod()).isEqualTo(VkCollectionMethod.OFFICIAL_API);
```

```java
.wallPostSnapshotRepository.saveAndFlush(VkWallPostSnapshot.builder()
        .collectionMethod(VkCollectionMethod.OFFICIAL_API)
        .build());
```

- [ ] **Step 2: Run the full VK/backend regression slice**

Run: `./mvnw -Dtest=HttpVkOfficialClientTest,VkOfficialMapperTest,IntegrationPersistenceTest,IntegrationApiTest test`
Expected: PASS with no references to fallback classes or enum values.

- [ ] **Step 3: Search for leftovers and remove them**

Run: `rg -n "Fallback|fallback|HYBRID|collectionMode|userAccessToken|fallbackBaseUrl|VkFallbackClient|VkFallbackPolicy|HttpVkFallbackClient|JsJsonExtractor|JsCommentStripper|FALLBACK" src/main/java src/test/java src/main/resources`
Expected: no fallback implementation references; only historical comments or intentionally retained DB semantics if any.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/ca/centranalytics/integration/channel/vk src/test/java/com/ca/centranalytics/integration/channel/vk src/test/java/com/ca/centranalytics/integration/domain/IntegrationPersistenceTest.java
git commit -m "refactor: delete vk fallback parsing"
```

### Task 5: Final Verification

**Files:**
- Verify current worktree only

- [ ] **Step 1: Run focused regression suite**

Run: `./mvnw -Dtest=IntegrationPropertiesTest,IntegrationApiTest,VkDiscoveryOrchestratorTest,VkDiscoveryOrchestratorSearchTest,VkAutoCollectionServiceTest,VkAutoCollectionSchedulerTest,HttpVkOfficialClientTest,VkOfficialMapperTest,IntegrationPersistenceTest,VkGroupManagementServiceTest test`
Expected: PASS with exit code 0.

- [ ] **Step 2: Run compile/package verification**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Review diff for scope control**

Run: `git diff --stat`
Expected: only VK official-only backend/config/test cleanup with no unrelated file reverts.
