# VK Group Management Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add admin operations to list groups, delete one or more existing VK groups with all linked group data, and trigger collection only for selected existing groups.

**Architecture:** Add a narrow `VkGroupManagementService` that resolves arbitrary operator input to existing `VkGroupCandidate` rows, then either launches existing post/comment crawl jobs or performs transactional cleanup of all group-linked records. Expose this through dedicated admin endpoints and keep the existing orchestrator and ingestion pipeline unchanged.

**Tech Stack:** Spring Boot, Spring MVC, Spring Data JPA, JUnit 5, existing VK discovery/crawl services

---

## Chunk 1: Resolver And Repository Surface

### Task 1: Identify DB lookup and cleanup gaps

**Files:**
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/repository/VkGroupCandidateRepository.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/repository/VkWallPostSnapshotRepository.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/repository/VkCommentSnapshotRepository.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/domain/repository/IntegrationSourceRepository.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkGroupManagementServiceTest.java`

- [ ] Add failing tests that require lookup by `screen_name`, bulk resolution of existing groups, and deletion of snapshots by group owner id.
- [ ] Run the focused resolver/service test and confirm the repository surface is insufficient.
- [ ] Add the minimal repository methods needed for local resolution and group-linked cleanup.
- [ ] Re-run the focused resolver/service test and confirm compilation-level repository failures are gone.

### Task 2: Add input normalization and group resolution

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkGroupIdentifierResolver.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkResolvedGroupSelection.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkGroupIdentifierResolverTest.java`

- [ ] Write failing resolver tests for `candidate.id`, `vkGroupId`, `screen_name`, `club/public`, and VK URL inputs.
- [ ] Run `VkGroupIdentifierResolverTest` and confirm failures.
- [ ] Implement the minimal parser/normalizer that resolves identifiers only against existing DB groups.
- [ ] Re-run `VkGroupIdentifierResolverTest` and confirm it passes.

## Chunk 2: Group Management Service

### Task 3: Add manual collect-by-groups service flow

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkGroupManagementService.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/VkGroupCollectRequest.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/VkGroupCollectResponse.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkDiscoveryQueryService.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkGroupManagementServiceTest.java`

- [ ] Add a failing service test showing that resolved groups create `GROUP_POSTS` and optional `POST_COMMENTS`, while unresolved identifiers are returned separately.
- [ ] Run `VkGroupManagementServiceTest` and confirm the collect path fails.
- [ ] Implement the minimal collect service on top of existing `VkCrawlCommandService`, deduplicating groups before job creation.
- [ ] Re-run `VkGroupManagementServiceTest` and confirm collect behavior passes.

### Task 4: Add delete-by-groups service flow

**Files:**
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkGroupManagementService.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/VkGroupDeleteRequest.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/VkGroupDeleteResponse.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkGroupManagementServiceTest.java`

- [ ] Add a failing service test showing that delete removes comment snapshots, post snapshots, the candidate row, and the linked `IntegrationSource`, while leaving `vk_user_candidate` untouched.
- [ ] Run `VkGroupManagementServiceTest` and confirm delete behavior fails.
- [ ] Implement the transactional delete flow with per-request cleanup ordering.
- [ ] Re-run `VkGroupManagementServiceTest` and confirm delete behavior passes.

## Chunk 3: Admin API

### Task 5: Expose collect and delete endpoints

**Files:**
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/VkDiscoveryAdminController.java`
  If this controller does not exist, create a focused admin controller in the same package pattern already used by VK manual APIs.
- Test: `src/test/java/com/ca/centranalytics/integration/api/IntegrationApiTest.java`

- [ ] Add failing MVC tests for `POST /api/admin/integrations/vk/groups/collect` and `DELETE /api/admin/integrations/vk/groups`, including partial-success responses.
- [ ] Run the focused API test selection and confirm failures.
- [ ] Implement the controller wiring and response mapping.
- [ ] Re-run the focused API tests and confirm they pass.

### Task 6: Expose list filtering if needed by the UI

**Files:**
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkDiscoveryQueryService.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/VkGroupCandidateResponse.java`
- Test: `src/test/java/com/ca/centranalytics/integration/api/IntegrationApiTest.java`

- [ ] Add a failing test only if the current list endpoint lacks fields needed for manual group operations.
- [ ] Run the focused API test and confirm the gap.
- [ ] Add only the minimal response/query changes needed for operator selection and deletion confirmation.
- [ ] Re-run the focused API test and confirm it passes.

## Chunk 4: Verification

### Task 7: Regression verification

**Files:**
- Modify: `docs/superpowers/specs/2026-04-10-vk-group-management-design.md`
  Only if implementation materially changes the agreed design.

- [ ] Run `HOME=/tmp MAVEN_USER_HOME=/tmp/.m2 sh ./mvnw -Dtest=VkGroupIdentifierResolverTest,VkGroupManagementServiceTest,IntegrationApiTest test`.
- [ ] Run `HOME=/tmp MAVEN_USER_HOME=/tmp/.m2 sh ./mvnw -Dtest=VkAutoCollectionServiceTest,HttpVkOfficialClientTest,VkDiscoveryOrchestratorSearchTest test` to guard current VK behavior.
- [ ] Run the broader Maven test slice if the environment allows it.
- [ ] Commit in small chunks with messages aligned to resolver, service, and API milestones.
