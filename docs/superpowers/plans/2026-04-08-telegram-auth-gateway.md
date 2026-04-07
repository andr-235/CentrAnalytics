# Telegram Auth Gateway Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Вынести Telegram user authorization в отдельный сервис на GramJS и подключить Spring Boot backend к нему через внутренний HTTP API.

**Architecture:** Новый `telegram-auth-gateway` живет отдельным Node.js/TypeScript сервисом в репозитории и отвечает только за `start`, `confirm`, `current`, `reset` auth flow. Java backend перестает заниматься MTProto authorization напрямую и вызывает gateway по HTTP внутри Docker-сети.

**Tech Stack:** Node.js, TypeScript, GramJS, Fastify, Docker, Spring Boot HTTP client, JUnit, Vitest/Jest

---

## Chunk 1: Gateway Scaffold

### Task 1: Create service skeleton

**Files:**
- Create: `telegram-auth-gateway/package.json`
- Create: `telegram-auth-gateway/tsconfig.json`
- Create: `telegram-auth-gateway/src/app.ts`
- Create: `telegram-auth-gateway/src/config/env.ts`
- Create: `telegram-auth-gateway/src/telegram/telegram-auth.types.ts`
- Create: `telegram-auth-gateway/Dockerfile`

- [ ] **Step 1: Write the failing bootstrap test**

Create a minimal HTTP smoke test for `GET /health` in:

`telegram-auth-gateway/src/app.test.ts`

- [ ] **Step 2: Run test to verify it fails**

Run: `npm --prefix telegram-auth-gateway test`

Expected: fail because project files do not exist yet.

- [ ] **Step 3: Create minimal project scaffold**

Add:
- `fastify`
- `gramjs` package `telegram`
- test runner used by the service

Implement `GET /health` returning `{ "status": "ok" }`.

- [ ] **Step 4: Run tests**

Run: `npm --prefix telegram-auth-gateway test`

Expected: pass for health route.

- [ ] **Step 5: Verify build**

Run: `npm --prefix telegram-auth-gateway run build`

Expected: successful TypeScript build.

- [ ] **Step 6: Commit**

```bash
git add telegram-auth-gateway
git commit -m "feat: scaffold telegram auth gateway"
```

## Chunk 2: Gateway Auth Domain

### Task 2: Implement transaction and session storage

**Files:**
- Create: `telegram-auth-gateway/src/telegram/telegram-session.repository.ts`
- Create: `telegram-auth-gateway/src/telegram/file-session-store.ts`
- Create: `telegram-auth-gateway/src/telegram/file-session-store.test.ts`

- [ ] **Step 1: Write failing repository tests**

Cover:
- save transaction
- get transaction by id
- save current session
- clear current session

- [ ] **Step 2: Run repository tests**

Run: `npm --prefix telegram-auth-gateway test -- file-session-store`

Expected: fail due to missing implementation.

- [ ] **Step 3: Implement file-backed storage**

Store in a dedicated local data directory:
- `current-session.json`
- `transactions.json`

Use atomic writes where reasonable.

- [ ] **Step 4: Re-run repository tests**

Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add telegram-auth-gateway/src/telegram
git commit -m "feat: add telegram auth storage"
```

### Task 3: Implement GramJS auth service

**Files:**
- Create: `telegram-auth-gateway/src/telegram/telegram-auth.service.ts`
- Create: `telegram-auth-gateway/src/telegram/telegram-auth.service.test.ts`

- [ ] **Step 1: Write failing service tests**

Mock GramJS client behavior for:
- `startSession`
- `confirmSession` with code
- `confirmSession` with password
- `getCurrentSession`
- `resetCurrentSession`

- [ ] **Step 2: Run tests**

Run: `npm --prefix telegram-auth-gateway test -- telegram-auth.service`

Expected: fail because service does not exist.

- [ ] **Step 3: Implement minimal service**

Methods:
- `startSession(phoneNumber)`
- `confirmSession(transactionId, code, password?)`
- `getCurrentSession()`
- `resetCurrentSession()`

Normalize errors to stable error codes.

- [ ] **Step 4: Re-run tests**

Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add telegram-auth-gateway/src/telegram
git commit -m "feat: implement telegram auth service"
```

## Chunk 3: Gateway HTTP API

### Task 4: Add REST endpoints

**Files:**
- Create: `telegram-auth-gateway/src/telegram/telegram-auth.controller.ts`
- Create: `telegram-auth-gateway/src/telegram/telegram-auth.routes.test.ts`
- Modify: `telegram-auth-gateway/src/app.ts`

- [ ] **Step 1: Write failing route tests**

Cover:
- `POST /session/start`
- `POST /session/confirm`
- `GET /session/current`
- `DELETE /session/current`

- [ ] **Step 2: Run route tests**

Run: `npm --prefix telegram-auth-gateway test -- telegram-auth.routes`

Expected: fail due to missing routes.

- [ ] **Step 3: Implement routes**

Return stable JSON payloads matching the spec.

- [ ] **Step 4: Re-run route tests**

Expected: pass.

- [ ] **Step 5: Run full gateway test suite**

Run: `npm --prefix telegram-auth-gateway test`

Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add telegram-auth-gateway/src
git commit -m "feat: expose telegram auth gateway api"
```

## Chunk 4: Backend Integration

### Task 5: Add backend configuration and HTTP client

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/channel/telegram/authgateway/config/TelegramAuthGatewayProperties.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/telegram/authgateway/client/TelegramAuthGatewayClient.java`
- Create: `src/main/java/com/ca/centranalytics/integration/channel/telegram/authgateway/dto/*.java`
- Modify: `src/main/resources/application.properties`
- Modify: `.env.example`

- [ ] **Step 1: Write failing backend client tests**

Add focused tests for mapping gateway responses and errors.

- [ ] **Step 2: Run focused tests**

Run: `HOME=/tmp MAVEN_USER_HOME=/tmp/.m2 sh ./mvnw -Dtest=TelegramAuthGatewayClientTest test`

Expected: fail because client is missing.

- [ ] **Step 3: Implement properties and client**

Required config:
- `integration.telegram-auth-gateway.base-url`
- optional timeouts

- [ ] **Step 4: Re-run focused tests**

Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ca/centranalytics/integration/channel/telegram/authgateway src/main/resources/application.properties .env.example
git commit -m "feat: add telegram auth gateway client"
```

### Task 6: Redirect existing Telegram admin endpoints to gateway

**Files:**
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/telegram/user/service/TelegramUserAuthService.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/telegram/user/controller/TelegramUserAdminController.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/telegram/user/api/*.java`
- Test: `src/test/java/com/ca/centranalytics/integration/api/IntegrationApiTest.java`

- [ ] **Step 1: Write failing controller/service tests**

Cover:
- start delegates to gateway
- code confirmation delegates to gateway
- password delegates to gateway
- current delegates to gateway

- [ ] **Step 2: Run focused tests**

Run: `HOME=/tmp MAVEN_USER_HOME=/tmp/.m2 sh ./mvnw -Dtest=IntegrationApiTest test`

Expected: fail due to old TDLib-backed behavior.

- [ ] **Step 3: Replace TDLib auth path**

Keep public API shape stable for frontend.

- [ ] **Step 4: Re-run focused tests**

Expected: pass or, if Testcontainers blocks full suite, pass focused MVC/service tests that don’t need Docker.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ca/centranalytics/integration/channel/telegram/user src/test/java/com/ca/centranalytics/integration/api/IntegrationApiTest.java
git commit -m "feat: route telegram auth through gateway"
```

## Chunk 5: Ops and Deployment

### Task 7: Add compose and CI/CD support

**Files:**
- Modify: `compose.prod.yaml`
- Modify: `compose.yaml`
- Modify: `.github/workflows/ci-cd.yml`
- Modify: `README.md`

- [ ] **Step 1: Write down service wiring changes in docs/tests as needed**

At minimum, document expected env vars and internal hostnames.

- [ ] **Step 2: Add gateway container to compose**

Internal only, no public port required.

- [ ] **Step 3: Add selective CI/CD build logic**

Build/deploy gateway image only when:
- `telegram-auth-gateway/**`
- related backend integration files
- compose/workflow files
change.

- [ ] **Step 4: Verify frontend/backend images unaffected when gateway-only files change**

Expected: selective pipeline behavior.

- [ ] **Step 5: Commit**

```bash
git add compose.prod.yaml compose.yaml .github/workflows/ci-cd.yml README.md
git commit -m "feat: deploy telegram auth gateway"
```

## Chunk 6: Verification

### Task 8: End-to-end validation

**Files:**
- Modify if needed: `README.md`

- [ ] **Step 1: Run local gateway tests**

Run:

```bash
npm --prefix telegram-auth-gateway test
npm --prefix telegram-auth-gateway run build
```

Expected: pass.

- [ ] **Step 2: Run backend verification**

Run:

```bash
HOME=/tmp MAVEN_USER_HOME=/tmp/.m2 sh ./mvnw -DskipTests compile
```

Expected: pass.

- [ ] **Step 3: Run local compose sanity check if feasible**

Bring up backend + gateway and manually verify:
- start session
- confirm code
- current session
- reset session

- [ ] **Step 4: Deploy to server**

Expected:
- gateway container reachable from app
- Telegram auth flow no longer depends on TDLib auth

- [ ] **Step 5: Final commit if verification required fixes**

```bash
git add .
git commit -m "fix: finalize telegram auth gateway integration"
```

Plan complete and saved to `docs/superpowers/plans/2026-04-08-telegram-auth-gateway.md`. Ready to execute?
