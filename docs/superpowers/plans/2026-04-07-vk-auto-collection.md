# VK Auto Collection Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add automatic VK region-group collection so newly discovered regional groups are periodically ingested into the unified messages feed without manual admin calls.

**Architecture:** Keep the existing VK discovery and ingestion pipeline intact and add a thin scheduling layer above it. The scheduler will read explicit `integration.vk.auto-collection.*` properties, search groups for the configured region, collect group posts, and then collect comments for the newly fetched posts using the existing orchestrator and job service.

**Tech Stack:** Spring Boot, JUnit 5, Mockito, Spring scheduling, existing VK discovery/ingestion services

---

### Task 1: Scheduler contract and configuration

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/config/VkAutoCollectionProperties.java`
- Modify: `src/main/resources/application.properties`
- Modify: `.env.example`
- Test: `src/test/java/com/ca/centranalytics/integration/config/IntegrationPropertiesTest.java`

- [ ] Add a failing properties binding test for `integration.vk.auto-collection.*`.
- [ ] Run the focused properties test and confirm it fails on missing binding.
- [ ] Add the minimal configuration properties class and application/env defaults.
- [ ] Re-run the focused properties test and confirm it passes.

### Task 2: Auto-collection service behavior

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkAutoCollectionService.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkAutoCollectionServiceTest.java`

- [ ] Add a failing unit test showing that enabled auto-collection triggers group search, post collection, and comment collection in sequence.
- [ ] Run the focused service test and confirm it fails for the expected missing behavior.
- [ ] Implement the minimal orchestration service on top of `VkCrawlJobService` and `VkDiscoveryOrchestrator`.
- [ ] Re-run the focused service test and confirm it passes.

### Task 3: Scheduler wiring

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/vk/service/VkAutoCollectionScheduler.java`
- Modify: `src/main/java/com/ca/centranalytics/CentrAnalyticsApplication.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/vk/VkAutoCollectionSchedulerTest.java`

- [ ] Add a failing scheduler test that confirms disabled mode does nothing and enabled mode delegates to the service.
- [ ] Run the focused scheduler test and confirm it fails.
- [ ] Add `@EnableScheduling` and wire the scheduled trigger with the configured delay.
- [ ] Re-run the focused scheduler test and confirm it passes.

### Task 4: Verification

**Files:**
- Modify: `README.md`

- [ ] Document how VK auto-collection is configured and what it does.
- [ ] Run the focused VK unit tests.
- [ ] Run the focused integration properties test.
- [ ] Run the full Maven test suite if the environment allows it.
