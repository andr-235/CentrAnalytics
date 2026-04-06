# VK Discovery And Ingestion Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a manual VK discovery and ingestion pipeline that can search groups and users by region, collect group posts and comments, and persist rich author profiles using official VK APIs with a controlled fallback path.

**Architecture:** Extend the existing Spring Boot `integration` module with VK-specific discovery tables, a crawl job orchestration layer, official and fallback VK clients, and admin endpoints for manually triggered collection. Normalize discovered VK data into the current `IntegrationSource`, `Conversation`, `Message`, and `ExternalUser` model while preserving VK-specific raw snapshots and job provenance.

**Tech Stack:** Spring Boot, Spring MVC, Spring Data JPA, Flyway, Jackson, JUnit 5, MockMvc, PostgreSQL

---

## Chunk 1: Persistence Foundation

### Task 1: Add VK crawl job and snapshot schema

**Files:**
- Create: `src/main/resources/db/migration/V5__create_vk_discovery_tables.sql`
- Modify: `src/test/java/com/ca/centranalytics/integration/domain/IntegrationPersistenceTest.java`

- [ ] Review existing integration tables in `src/main/resources/db/migration/V2__create_integration_tables.sql` and `src/main/resources/db/migration/V4__scope_integration_entities_by_source.sql`
- [ ] Add tables for `vk_crawl_job`, `vk_group_candidate`, `vk_user_candidate`, `vk_wall_post_snapshot`, and `vk_comment_snapshot`
- [ ] Add unique constraints for `vk_group_id`, `vk_user_id`, `owner_id + post_id`, and `owner_id + post_id + comment_id`
- [ ] Extend persistence tests to verify inserts and deduplication behavior for the new VK entities
- [ ] Run: `./mvnw -Dtest=IntegrationPersistenceTest test`

### Task 2: Add JPA entities and repositories for VK discovery data

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/domain/VkCrawlJob.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/domain/VkGroupCandidate.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/domain/VkUserCandidate.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/domain/VkWallPostSnapshot.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/domain/VkCommentSnapshot.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/domain/VkCrawlJobStatus.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/domain/VkCrawlJobType.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/domain/VkMatchSource.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/domain/VkCollectionMethod.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/repository/VkCrawlJobRepository.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/repository/VkGroupCandidateRepository.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/repository/VkUserCandidateRepository.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/repository/VkWallPostSnapshotRepository.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/repository/VkCommentSnapshotRepository.java`
- Test: `src/test/java/com/ca/centranalytics/integration/domain/IntegrationPersistenceTest.java`

- [ ] Add enums for job status, job type, region match source, and collection method
- [ ] Implement focused entities with timestamps and raw JSON fields
- [ ] Add Spring Data repositories with lookup methods required for upsert and job reads
- [ ] Run: `./mvnw -Dtest=IntegrationPersistenceTest test`

## Chunk 2: API Contracts And Job Lifecycle

### Task 3: Add admin API request and response DTOs for VK crawl operations

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/SearchVkGroupsRequest.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/SearchVkUsersRequest.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/CollectVkGroupPostsRequest.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/CollectVkPostCommentsRequest.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/EnrichVkUsersRequest.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/VkCrawlJobResponse.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/api/VkCrawlJobStatusResponse.java`
- Test: `src/test/java/com/ca/centranalytics/integration/api/IntegrationApiTest.java`

- [ ] Define request DTOs with validation for region, limits, IDs, and collection mode
- [ ] Define response DTOs for job creation and job status polling
- [ ] Add API tests for invalid payloads and response shapes
- [ ] Run: `./mvnw -Dtest=IntegrationApiTest test`

### Task 4: Add VK crawl job services and admin endpoints

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkCrawlJobService.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkCrawlCommandService.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkJobQueryService.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/admin/controller/IntegrationAdminController.java`
- Test: `src/test/java/com/ca/centranalytics/integration/api/IntegrationApiTest.java`

- [ ] Add service methods to create jobs for group search, user search, post collection, comment collection, and user enrichment
- [ ] Return immediate job metadata from admin endpoints instead of blocking on long crawls
- [ ] Add a `GET` endpoint for job status and statistics
- [ ] Keep endpoint naming aligned with the approved manual operation model
- [ ] Run: `./mvnw -Dtest=IntegrationApiTest test`

## Chunk 3: Official VK Client And Matching Logic

### Task 5: Add official VK client abstraction and transport DTOs

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/VkOfficialClient.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/HttpVkOfficialClient.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/dto/VkGroupSearchResult.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/dto/VkUserSearchResult.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/dto/VkWallPostResult.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/dto/VkCommentResult.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/config/VkProperties.java`
- Test: `src/test/java/com/ca/centranalytics/integration/config/IntegrationPropertiesTest.java`

- [ ] Extend `VkProperties` with API version, base URL, user token support, and request timeout settings
- [ ] Introduce an official client interface so orchestration code does not depend on transport details
- [ ] Implement DTOs that represent only the fields needed by the first version
- [ ] Add configuration binding tests for the new VK properties
- [ ] Run: `./mvnw -Dtest=IntegrationPropertiesTest test`

### Task 6: Add region matching and official-response mappers

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkRegionMatcher.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkOfficialGroupCandidateMapper.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkOfficialUserCandidateMapper.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkWallSnapshotMapper.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkCommentSnapshotMapper.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkRegionMatcherTest.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkOfficialMapperTest.java`

- [ ] Write failing unit tests for structured region matches and text-based fallback matches
- [ ] Implement deterministic region classification that reports `match_source`
- [ ] Map official VK responses into candidate and snapshot entities with raw JSON preserved
- [ ] Run: `./mvnw -Dtest=VkRegionMatcherTest,VkOfficialMapperTest test`

## Chunk 4: Fallback Path

### Task 7: Add fallback client abstraction and policy gate

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/client/VkFallbackClient.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkFallbackPolicy.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/NoopVkFallbackClient.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkFallbackPolicyTest.java`

- [ ] Define a fallback client interface without implementing a browser or scraper in the first slice
- [ ] Add explicit policy rules that decide when fallback is allowed
- [ ] Default the fallback client to a no-op implementation until a real collector is approved and built
- [ ] Run: `./mvnw -Dtest=VkFallbackPolicyTest test`

### Task 8: Wire hybrid collection orchestration

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkDiscoveryOrchestrator.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkCrawlCommandService.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkDiscoveryOrchestratorTest.java`

- [ ] Implement orchestration flow that calls the official client first
- [ ] Invoke the fallback client only when `VkFallbackPolicy` allows it
- [ ] Update job counters, warnings, and terminal status as each stage completes
- [ ] Run: `./mvnw -Dtest=VkDiscoveryOrchestratorTest test`

## Chunk 5: Normalization Into Existing Integration Domain

### Task 9: Add VK normalization services for sources, authors, posts, and comments

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkSourceNormalizationService.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkAuthorNormalizationService.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkPostIngestionMapper.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkCommentIngestionMapper.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/ingestion/service/IntegrationIngestionService.java`
- Test: `src/test/java/com/ca/centranalytics/integration/ingestion/IntegrationIngestionServiceTest.java`

- [ ] Map group candidates into `IntegrationSource` records with VK settings JSON
- [ ] Map rich VK profile data into `ExternalUser.metadataJson` while preserving normalized names and profile URL
- [ ] Convert wall posts and comments into `InboundIntegrationEvent` objects that can reuse the existing ingestion pipeline
- [ ] Keep idempotency by generating stable event IDs for posts and comments
- [ ] Run: `./mvnw -Dtest=IntegrationIngestionServiceTest test`

### Task 10: Persist and upsert VK candidates and snapshots

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkCandidatePersistenceService.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkSnapshotPersistenceService.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkPersistenceServiceTest.java`

- [ ] Implement upsert logic for groups, users, posts, and comments
- [ ] Preserve `collection_method`, `match_source`, and raw JSON for every persisted VK-specific record
- [ ] Add tests for deduplication and metadata updates on repeated collection
- [ ] Run: `./mvnw -Dtest=VkPersistenceServiceTest test`

## Chunk 6: API Finish, Documentation, And Verification

### Task 11: Add controller-level tests for the new VK admin endpoints

**Files:**
- Create: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkAdminControllerTest.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/admin/controller/IntegrationAdminController.java`

- [ ] Add tests for each new endpoint and job-status polling
- [ ] Verify security expectations match the existing admin API posture
- [ ] Run: `./mvnw -Dtest=VkAdminControllerTest test`

### Task 12: Document configuration and operating flow

**Files:**
- Modify: `.env.example`
- Modify: `README.md`
- Create: `docs/superpowers/specs/2026-04-06-vk-discovery-ingestion-design.md`
- Create: `docs/superpowers/plans/2026-04-06-vk-discovery-ingestion.md`

- [ ] Document required VK tokens, optional user token, and fallback feature flag
- [ ] Document the manual admin endpoints and expected workflow for operators
- [ ] Save the approved design and implementation plan in the repository
- [ ] Run: `./mvnw test`

Plan complete and saved to `docs/superpowers/plans/2026-04-06-vk-discovery-ingestion.md`. Ready to execute?
