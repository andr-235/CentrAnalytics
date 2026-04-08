# Telegram Collector Gateway Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Перенести Telegram message collection с `TDLib` на `GramJS`, используя уже работающий `telegram-auth-gateway` и существующий backend ingestion pipeline.

**Architecture:** `telegram-auth-gateway` становится владельцем и auth, и long-lived Telegram client. Spring Boot backend принимает только нормализованные Telegram события через внутренний ingestion endpoint и сохраняет их обычным `IntegrationIngestionService`.

**Tech Stack:** Node.js, TypeScript, GramJS, Fastify, Spring Boot, RestClient, JUnit, Vitest

---

## Chunk 1: Collector Runtime In Gateway

### Task 1: Add collector runtime manager

**Files:**
- Create: `telegram-auth-gateway/src/telegram/telegram-collector.service.ts`
- Create: `telegram-auth-gateway/src/telegram/telegram-collector.types.ts`
- Create: `telegram-auth-gateway/src/telegram/telegram-collector.service.test.ts`

- [ ] **Step 1: Write failing collector lifecycle tests**

Cover:
- start collector from saved session
- stop collector on reset
- report `STOPPED/STARTING/RUNNING/FAILED`

- [ ] **Step 2: Run focused tests**

Run: `npm --prefix telegram-auth-gateway test -- telegram-collector.service`

Expected: fail because collector runtime is missing.

- [ ] **Step 3: Implement runtime manager**

Responsibilities:
- build `TelegramClient` from saved `StringSession`
- connect/disconnect cleanly
- expose collector status

- [ ] **Step 4: Re-run tests**

Expected: pass.

## Chunk 2: Backend Internal Ingestion Entry Point

### Task 2: Add internal Telegram ingestion endpoint

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/telegram/gateway/api/*.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/telegram/gateway/controller/TelegramGatewayIngestionController.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/telegram/gateway/service/TelegramGatewayIngestionService.java`
- Test: `src/test/java/com/ca/centranalytics/integration/channel/telegram/gateway/TelegramGatewayIngestionControllerTest.java`

- [ ] **Step 1: Write failing MVC test**

Cover:
- accepted trusted Telegram payload
- passed into `IntegrationIngestionService`

- [ ] **Step 2: Run focused test**

Run: `HOME=/tmp MAVEN_USER_HOME=/tmp/.m2 sh ./mvnw -Dtest=TelegramGatewayIngestionControllerTest test`

Expected: fail because controller is missing.

- [ ] **Step 3: Implement internal endpoint**

Route idea:
- `POST /api/internal/integrations/telegram-user/events`

Keep it internal-only and not exposed through frontend nginx.

- [ ] **Step 4: Re-run focused test**

Expected: pass.

## Chunk 3: Event Mapping In Gateway

### Task 3: Map GramJS messages into backend ingestion payload

**Files:**
- Create: `telegram-auth-gateway/src/telegram/telegram-ingestion.mapper.ts`
- Create: `telegram-auth-gateway/src/telegram/telegram-ingestion.mapper.test.ts`

- [ ] **Step 1: Write failing mapper tests**

Cover:
- direct chats
- groups
- text messages
- stable event id generation

- [ ] **Step 2: Run focused tests**

Run: `npm --prefix telegram-auth-gateway test -- telegram-ingestion.mapper`

Expected: fail because mapper is missing.

- [ ] **Step 3: Implement mapper**

Match the current Java mapper semantics closely enough that `IntegrationIngestionService` can reuse the event without schema rewrites.

- [ ] **Step 4: Re-run focused tests**

Expected: pass.

## Chunk 4: Wire Collector To Backend

### Task 4: Send collected events into backend

**Files:**
- Create: `telegram-auth-gateway/src/backend/backend-ingestion.client.ts`
- Create: `telegram-auth-gateway/src/backend/backend-ingestion.client.test.ts`
- Modify: `telegram-auth-gateway/src/telegram/telegram-collector.service.ts`
- Modify: `telegram-auth-gateway/src/server.ts`
- Modify: `telegram-auth-gateway/src/config/env.ts`

- [ ] **Step 1: Write failing integration-facing tests**

Cover:
- collector receives a Telegram update
- gateway POSTs normalized event to backend internal endpoint

- [ ] **Step 2: Run focused tests**

Run: `npm --prefix telegram-auth-gateway test -- backend-ingestion`

Expected: fail because backend client is missing.

- [ ] **Step 3: Implement backend ingestion client**

Required env:
- `BACKEND_INGESTION_BASE_URL`
- optional internal auth header/token if needed

- [ ] **Step 4: Re-run tests**

Expected: pass.

## Chunk 5: Switch Runtime Off TDLib

### Task 5: Disable old TDLib bootstrap path for collection

**Files:**
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/telegram/user/service/TelegramUserSessionBootstrap.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/telegram/user/service/TelegramTdLibClientManager.java`
- Modify: `README.md`
- Modify: `.env.example`

- [ ] **Step 1: Write failing regression test or focused startup test**

Prove that Telegram auth no longer depends on TDLib runtime startup.

- [ ] **Step 2: Implement the cutover**

Goal:
- auth + collector live in gateway
- backend no longer tries to resume TDLib sessions for Telegram user collection

- [ ] **Step 3: Re-run focused backend and gateway tests**

Expected: pass.

## Chunk 6: Deployment

### Task 6: Add runtime config and deploy verification

**Files:**
- Modify: `compose.prod.yaml`
- Modify: `.github/workflows/ci-cd.yml`
- Modify: `README.md`

- [ ] **Step 1: Add gateway collector env**

Examples:
- `BACKEND_INGESTION_BASE_URL`
- collector auto-start toggle

- [ ] **Step 2: Add health/status verification**

Deployment should verify:
- gateway health
- backend health
- collector status endpoint if available

- [ ] **Step 3: Run final verification**

Run:
- `npm --prefix telegram-auth-gateway test`
- `npm --prefix telegram-auth-gateway run build`
- focused backend tests for internal ingestion endpoint

