# CentrAnalytics

## Project Overview

CentrAnalytics is a **Spring Boot 4.0.5 analytics platform** that collects, normalizes, and analyzes messages from multiple social media and messaging platforms (VK, Telegram, WhatsApp via Wappi, Max). It provides a unified ingestion pipeline, REST APIs for querying conversations/messages/metrics, and integrates with external services like a Telegram auth gateway and VK API.

The repository is a **multi-component monorepo** with three deployable services:

- **Backend** — Spring Boot 4.0.5 (Java 26) REST API
- **Frontend** — React 19 + Vite + TypeScript SPA with nginx
- **Telegram Auth Gateway** — Fastify + GramJS (MTProto) service

### Tech Stack

- **Java 26**, **Spring Boot 4.0.5**
- **Maven** (wrapper: `./mvnw`)
- **PostgreSQL 17** (primary datastore), **Flyway** (11 migrations)
- **Redis** (sessions; disabled in production health checks)
- **JWT** authentication (jjwt 0.11.5)
- **Lombok** (`@RequiredArgsConstructor`, etc.)
- **Testcontainers**, **JUnit 5**, **MockMvc** (testing — 39 test files)
- **Docker Compose** (local & production orchestration — 5 services)
- **Springdoc OpenAPI** (Swagger UI at `/swagger-ui.html`)
- **Telegram GramJS** (telegram-auth-gateway) for Telegram user sessions
- **VK Java SDK** (1.0.16) for VK API integration
- **React 19**, **Vite 7**, **TypeScript 5.9** (frontend)
- **Fastify 5**, **Zod 4** (telegram-auth-gateway)

---

## Project Structure

```
CentrAnalytics/
├── src/main/java/com/ca/centranalytics/
│   ├── auth/                          # JWT authentication
│   │   ├── config/                    #   JwtProperties
│   │   ├── controller/                #   AuthController
│   │   ├── dto/                       #   AuthRequest, AuthResponse, RegisterRequest
│   │   ├── exception/                 #   GlobalExceptionHandler
│   │   ├── security/                  #   JwtService, JwtAuthenticationFilter, SecurityConfig
│   │   │                              #   WebhookSignatureFilter, InternalTokenFilter
│   │   ├── service/                   #   AuthService
│   │   └── validation/                #   StrongPasswordValidator
│   ├── common/                        # Shared configuration
│   │   └── config/                    #   CorsProperties
│   ├── user/                          # User management
│   │   ├── controller/                #   TestController
│   │   ├── entity/                    #   User, Role (JPA)
│   │   └── repository/                #   UserRepository
│   └── integration/                   # Core integration engine (largest module)
│       ├── admin/                     #   Admin REST for integration management
│       ├── api/                       #   Public REST API (conversations, messages, overview)
│       ├── channel/                   #   Platform-specific channels
│       │   ├── vk/                    #     VK: auto-collection, discovery, group management
│       │   ├── telegram/              #     Telegram: gateway client, ingestion mapping
│       │   ├── max/wappi/             #     Max: webhook ingestion
│       │   └── whatsapp/wappi/        #     WhatsApp/Wappi: webhook ingestion
│       ├── config/                    #   IntegrationProperties, JacksonConfig
│       ├── domain/                    #   JPA entities (14+), repositories, projections
│       └── ingestion/                 #   Unified ingestion pipeline (normalize -> resolve -> persist)
├── frontend/                          # React + Vite + TypeScript SPA
│   ├── src/features/                  # Feature modules
│   │   ├── auth/                      #   AuthPage, auth.api.ts
│   │   ├── dashboard/                 #   DashboardPage, dashboard.api.ts
│   │   ├── integrations/              #   IntegrationsPage, TelegramSessionPage, etc.
│   │   ├── overview/                  #   OverviewPage, overview.api.ts
│   │   └── shell/                     #   AppShell, navigation
│   └── nginx.conf                     # Production nginx proxy config
└── telegram-auth-gateway/             # Telegram auth & collection gateway
    └── src/                           # Fastify + GramJS server
```

### Key Domain Entities

`IntegrationSource`, `IntegrationAccount`, `Conversation`, `ExternalUser`, `Message`, `MessageAttachment`, `MessageAttachmentContent`, `RawEvent`, `IngestionCheckpoint`, `TelegramUserSession`, plus VK discovery entities (`VkCrawlJob`, `VkGroupCandidate`, `VkUserCandidate`, `VkWallPostSnapshot`, `VkCommentSnapshot`).

### Database Migrations (Flyway)

Located in `src/main/resources/db/migration/`:

| Migration | Description |
|-----------|-------------|
| `V1` | Create `users` table |
| `V2` | Create integration tables (source, account, conversation, external_user, raw_event, message, attachment, checkpoint) |
| `V3` | Create `telegram_user_session` table |
| `V4` | Scope integration entities by `source_id` + `source_external_id` |
| `V5` | Create VK discovery tables (crawl_job, group_candidate, user_candidate, wall_post, comment) |
| `V6` | Extend VK user candidate profile fields |
| `V7` | Create `message_attachment_content` table (binary content) |
| `V8` | Add message listing composite indexes |
| `V9` | Add user audit columns |
| `V10` | Add VK group post collection block |
| `V11` | Drop Telegram TDLib session table |

---

## Building and Running

### Prerequisites

- **Java 26** — verify with `java -version`
- **Node.js 22** — for frontend and telegram-auth-gateway subprojects
- **Docker & Docker Compose** — for local PostgreSQL and full-stack runs

### Commands

```bash
# --- Backend ---

# Run locally (requires PostgreSQL available)
./mvnw spring-boot:run

# Run full test suite (39 test files)
./mvnw test

# Build runnable JAR
./mvnw clean package

# --- Frontend ---
cd frontend && npm ci
npm run dev               # Vite dev server
npm run build             # Production build
npm test                  # Vitest test suite

# --- Telegram Auth Gateway ---
cd telegram-auth-gateway && npm ci
npm run dev               # Fastify dev server
npm run build             # TypeScript compilation
npm test                  # Vitest test suite

# --- Docker Compose ---

# Start local stack (PostgreSQL + Redis + app)
docker compose up --build

# Production stack (5 services)
docker compose -f compose.prod.yaml up --build
```

### Configuration

Copy `.env.example` to `.env` and fill in real secrets. Key variables:

| Variable | Purpose |
|----------|---------|
| `JWT_SECRET` | Base64-encoded JWT signing secret (required, min 64 bytes for HS512) |
| `POSTGRES_*` | Database credentials |
| `VK_*` | VK API integration settings |
| `TELEGRAM_API_ID/HASH` | Telegram credentials |
| `TELEGRAM_AUTH_GATEWAY_*` | External Telegram auth gateway URL/timeouts |
| `TELEGRAM_GATEWAY_INGESTION_INTERNAL_TOKEN` | Service-to-service token for gateway ingestion |
| `WAPPI_WEBHOOK_PATH` | WhatsApp/Max webhook path |

See `.env.example` for full list of configurable properties.

---

## Architecture Highlights

### Production Topology (`compose.prod.yaml`)

```
frontend (nginx:18080) ─┬─ /auth ──────────┐
                        ├─ /api ───────────┤
                        ├─ /actuator ──────┤── app (Spring Boot:8080)
                        ├─ /swagger-ui ────┤
                        ├─ /api-docs ──────┤
                        └─ /v3/api-docs ───┘

  telegram-auth-gateway (8091) ── GramJS/MTProto for Telegram auth + collection
  centranalytics-tgproxy (Xray) ── Telegram egress proxy
  postgres (17) ── Primary datastore
```

The frontend `nginx` proxies all backend paths, keeping the browser on a **single origin** — no CORS needed in production.

### Security Filter Chain

The backend uses a three-filter security chain (ordered):

1. **`WebhookSignatureFilter`** — validates incoming webhook signatures (platform webhooks)
2. **`InternalTokenFilter`** — validates `X-Internal-Token` header (service-to-service calls)
3. **`JwtAuthenticationFilter`** — validates JWT bearer tokens (user-facing API)

Public endpoints (no auth required): `/auth/register`, `/auth/login`, `/api/integrations/webhooks/**`, `/actuator/health/**`, `/swagger-ui/**`, `/api-docs/**`.

### Ingestion Pipeline

External platforms send webhooks or are polled via scheduled tasks. Events flow through:

1. **Platform webhook controller** (VK, Telegram, Wappi, Max)
2. **Inbound event mapper** — normalizes to `InboundIntegrationEvent`
3. **IntegrationIngestionService** — resolves conversation, external user, deduplication
4. **MessagePersistenceService** — persists message + attachments + content
5. **MessageCapturedEvent** — Spring application event for downstream processing

### Telegram Integration

Two modes are supported:
- **Auth Gateway** (production default) — external service (`telegram-auth-gateway`) handles GramJS/MTProto authorization; the gateway also collects messages and pushes them to the backend ingestion endpoint
- **Direct TDLib** — built-in Telegram user client (legacy)

The backend exposes `POST /api/internal/integrations/telegram-user/events` for gateway ingestion, protected by `X-Internal-Token`.

### VK Auto Collection

Scheduled region-based collection without webhooks:
- Periodically searches VK groups for configured region
- Collects recent wall posts and comments
- Normalizes into shared integration messages feed
- Controlled by `VK_AUTO_COLLECTION_*` properties (default 15 min interval)

---

## Testing

### Backend (JUnit 5, 39 test files)

- **Unit tests**: `*Test.java` — JUnit 5, MockMvc
- **Integration tests**: `*IntegrationTest.java` — Testcontainers, Spring Boot test
- **Examples**: `AuthControllerIntegrationTest`, `TelegramGatewayIngestionControllerTest`, `WappiWebhookControllerTest`

```bash
./mvnw test
```

Tests use Testcontainers for PostgreSQL isolation. Run before opening PRs.

### Frontend (Vitest + Testing Library)

```bash
cd frontend && npm test
```

### Telegram Auth Gateway (Vitest)

```bash
cd telegram-auth-gateway && npm test
```

---

## CI/CD

GitHub Actions workflow (`.github/workflows/ci-cd.yml`) for single-developer flow:

### Path-Based Selective Builds

| Changed Path | Triggered Jobs |
|--------------|----------------|
| `src/**`, `pom.xml`, `Dockerfile`, `mvnw`, `.mvn/**` | Backend test + build |
| `frontend/**` | Frontend test + build |
| `telegram-auth-gateway/**` | Gateway test + build |
| `compose.prod.yaml`, `.env.example`, `README.md` | Deploy-only flag |

### Pipeline Stages

- **PRs to `main`**: selective checks for changed areas (no deployment)
- **Pushes to `main`**: selective tests → build → push to `ghcr.io` → deploy changed services

Deployment runs on a self-hosted runner on the target server. Required secrets: `DEPLOY_PATH`, `GHCR_USERNAME`, `GHCR_TOKEN`.

---

## Development Conventions

- **Indentation**: 4 spaces (Java), 2 spaces (TypeScript/JS)
- **Naming**: `PascalCase` classes, `camelCase` methods/fields, `UPPER_SNAKE_CASE` constants
- **Suffixes**: `*Controller`, `*Service`, `*Repository`, `*Request`/`*Response` (DTOs)
- **Injection**: constructor injection via `@RequiredArgsConstructor`
- **Packages**: feature-oriented under `com.ca.centranalytics.<domain>`
- **Commits**: imperative prefix style — `feat:`, `fix:`, `chore:`
- **PRs**: explain change, note config/migration impact, link issue, include request/response samples for API changes
- **When changing a subproject**: note which service is affected (backend, frontend, or telegram-auth-gateway)

---

## Security Notes

- Never commit real secrets — use `.env.example` as template
- Internal ingestion endpoints (`/api/internal/*`) protected by `X-Internal-Token`
- **Three-filter security chain**: WebhookSignatureFilter → InternalTokenFilter → JwtAuthenticationFilter
- Flyway migrations are forward-only; review carefully
- Auth controller and webhook endpoints are externally exposed — review security config changes
- `centranalytics-tgproxy` must be managed via `docker compose`, not ad-hoc processes
- `JWT_SECRET` must be at least 64 bytes (HS512 algorithm)
