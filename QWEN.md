# CentrAnalytics

## Project Overview

CentrAnalytics is a **Spring Boot 4.0 analytics platform** that collects, normalizes, and analyzes messages from multiple social media and messaging platforms (VK, Telegram, WhatsApp via Wappi, Max). It provides a unified ingestion pipeline, REST APIs for querying conversations/messages/metrics, and integrates with external services like a Telegram auth gateway and VK API.

### Tech Stack

- **Java 26**, **Spring Boot 4.0.5**
- **Maven** (wrapper: `./mvnw`)
- **PostgreSQL** (primary datastore), **Flyway** (migrations)
- **Redis** (sessions; disabled in production health checks)
- **JWT** authentication (jjwt 0.11.5)
- **Lombok** (`@RequiredArgsConstructor`, etc.)
- **Testcontainers**, **JUnit 5**, **MockMvc** (testing)
- **Docker Compose** (local & production orchestration)
- **Springdoc OpenAPI** (Swagger UI at `/swagger-ui.html`)
- **Telegram TDLib** (tdlight-java) for Telegram user sessions
- **VK Java SDK** for VK API integration

---

## Project Structure

```
src/main/java/com/ca/centranalytics/
├── auth/                          # JWT authentication
│   ├── config/                    #   JwtProperties
│   ├── controller/                #   AuthController
│   ├── dto/                       #   AuthRequest, AuthResponse, RegisterRequest
│   ├── exception/                 #   GlobalExceptionHandler
│   ├── security/                  #   JwtService, JwtAuthenticationFilter, SecurityConfig
│   └── service/                   #   AuthService
├── user/                          # User management
│   ├── controller/                #   TestController
│   ├── entity/                    #   User, Role (JPA)
│   └── repository/                #   UserRepository
└── integration/                   # Core integration engine (largest module)
    ├── admin/                     #   Admin REST for integration management
    ├── api/                       #   Public REST API (conversations, messages, overview)
    ├── channel/                   #   Platform-specific channels
    │   ├── vk/                    #     VK: auto-collection, discovery, group management
    │   ├── telegram/              #     Telegram: TDLib client, auth gateway, ingestion
    │   ├── max/wappi/             #     Max: webhook ingestion
    │   └── whatsapp/wappi/        #     WhatsApp/Wappi: webhook ingestion
    ├── config/                    #   IntegrationProperties, JacksonConfig
    ├── domain/                    #   JPA entities (14 entities), repositories, projections
    └── ingestion/                 #   Unified ingestion pipeline (normalize -> resolve -> persist)
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

---

## Building and Running

### Prerequisites

- **Java 26** — verify with `java -version`
- **Docker & Docker Compose** — for local PostgreSQL and full-stack runs

### Commands

```bash
# Run locally (requires PostgreSQL available)
./mvnw spring-boot:run

# Run full test suite
./mvnw test

# Build runnable JAR
./mvnw clean package

# Start local stack (PostgreSQL + app via Docker Compose)
docker compose up --build

# Production stack
docker compose -f compose.prod.yaml up --build
```

### Configuration

Copy `.env.example` to `.env` and fill in real secrets. Key variables:

| Variable | Purpose |
|----------|---------|
| `JWT_SECRET` | Base64-encoded JWT signing secret (required) |
| `POSTGRES_*` | Database credentials |
| `VK_*` | VK API integration settings |
| `TELEGRAM_USER_API_ID/HASH` | Telegram TDLib credentials |
| `TELEGRAM_AUTH_GATEWAY_*` | External Telegram auth gateway URL/timeouts |
| `TELEGRAM_GATEWAY_INGESTION_INTERNAL_TOKEN` | Service-to-service token for gateway ingestion |
| `WAPPI_WEBHOOK_PATH` | WhatsApp webhook path |

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
                        
  telegram-auth-gateway (8091) ── GramJS/MTProto for Telegram auth
  centranalytics-tgproxy (Xray) ── Telegram egress proxy
  postgres (17) ── Primary datastore
```

The frontend `nginx` proxies all backend paths, keeping the browser on a **single origin** — no CORS needed in production.

### Ingestion Pipeline

External platforms send webhooks or are polled via scheduled tasks. Events flow through:

1. **Platform webhook controller** (VK, Telegram, Wappi, Max)
2. **Inbound event mapper** — normalizes to `InboundIntegrationEvent`
3. **IntegrationIngestionService** — resolves conversation, external user, deduplication
4. **MessagePersistenceService** — persists message + attachments + content
5. **MessageCapturedEvent** — Spring application event for downstream processing

### Telegram Integration

Two modes are supported:
- **TDLib direct** — built-in Telegram user client with session management
- **Auth Gateway** — external service (`telegram-auth-gateway`) handles GramJS/MTProto authorization; the gateway also collects messages and pushes them to the backend ingestion endpoint

The backend exposes `POST /api/internal/integrations/telegram-user/events` for gateway ingestion, protected by `X-Internal-Token`.

### VK Auto Collection

Scheduled region-based collection without webhooks:
- Periodically searches VK groups for configured region
- Collects recent wall posts and comments
- Normalizes into shared integration messages feed
- Controlled by `VK_AUTO_COLLECTION_*` properties (default 15 min interval)

---

## Testing

- **Unit tests**: `*Test.java` — JUnit 5, MockMvc
- **Integration tests**: `*IntegrationTest.java` — Testcontainers, Spring Boot test
- **Examples**: `AuthControllerIntegrationTest`

```bash
./mvnw test
```

Tests use Testcontainers for PostgreSQL isolation. Run before opening PRs.

---

## CI/CD

GitHub Actions workflow for single-developer flow:

- **PRs to `main`**: selective checks for changed areas
- **Pushes to `main`**: selective tests → build → push to `ghcr.io` → deploy changed services

Deployment runs on a self-hosted runner on the target server. Required secrets: `DEPLOY_PATH`, `GHCR_USERNAME`, `GHCR_TOKEN`.

---

## Development Conventions

- **Indentation**: 4 spaces
- **Naming**: `PascalCase` classes, `camelCase` methods/fields, `UPPER_SNAKE_CASE` constants
- **Suffixes**: `*Controller`, `*Service`, `*Repository`, `*Request`/`*Response` (DTOs)
- **Injection**: constructor injection via `@RequiredArgsConstructor`
- **Packages**: feature-oriented under `com.ca.centranalytics.<domain>`
- **Commits**: imperative prefix style — `feat:`, `fix:`, `chore:`
- **PRs**: explain change, note config/migration impact, link issue, include request/response samples for API changes

---

## Security Notes

- Never commit real secrets — use `.env.example` as template
- Internal ingestion endpoints (`/api/internal/*`) protected by `X-Internal-Token`
- Flyway migrations are forward-only; review carefully
- Auth controller and webhook endpoints are externally exposed — review security config changes
- `tgproxy` must be managed via `docker compose`, not ad-hoc processes
