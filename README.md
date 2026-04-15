# CentrAnalytics

> Multi-platform social media and messaging analytics service — collect, normalize, and analyze conversations from VK, Telegram, WhatsApp (Wappi), and Max (Wappi) through a unified ingestion pipeline.

[![Java](https://img.shields.io/badge/Java-26-ED8B00?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.5-6DB33F?logo=spring)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?logo=postgresql)](https://www.postgresql.org/)
[![CI/CD](https://github.com/your-org/CentrAnalytics/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/your-org/CentrAnalytics/actions/workflows/ci-cd.yml)
[![License](https://img.shields.io/badge/License-proprietary-gray)]()

---

## Quick Start

```bash
# 1. Clone and configure
git clone <repo-url> && cd CentrAnalytics
cp .env.example .env          # edit JWT_SECRET and platform credentials

# 2. Start local stack (PostgreSQL + Redis + app)
docker compose up --build

# 3. Open the app
# Backend API:     http://localhost:8080
# Swagger UI:      http://localhost:8080/swagger-ui.html
# Actuator health: http://localhost:8080/actuator/health
```

See [Building and Running](#building-and-running) for detailed setup instructions.

---

## Supported Platforms

| Platform  | Integration Method      | Description                                       |
|-----------|-------------------------|---------------------------------------------------|
| **VK**    | Auto-collection (poll)  | Scheduled region-based discovery, wall posts, comments |
| **Telegram** | Auth Gateway + Collector | GramJS/MTProto authorization and message collection via external gateway |
| **WhatsApp** | Wappi webhook          | Inbound message webhook via Wappi service         |
| **Max**   | Wappi webhook           | Inbound message webhook via Wappi service         |

All platforms feed into a **unified ingestion pipeline** that normalizes events into a common data model (conversations, messages, external users, attachments).

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Production Topology                          │
│                                                                     │
│  ┌──────────────┐                                                   │
│  │   Frontend    │  React 19 + Vite + nginx (port 18080)            │
│  │  (nginx SPA)  │  Proxies /auth, /api, /actuator, /swagger-ui     │
│  └──────┬───────┘                                                   │
│         │ single origin — no CORS needed                            │
│  ┌──────▼───────┐    ┌──────────────────────┐                       │
│  │     App       │◄───│ centranalytics-tgproxy│  Xray egress proxy   │
│  │ Spring Boot   │    │  (Telegram egress)    │  for MTProto traffic  │
│  │   :8080       │    └──────────────────────┘                       │
│  └──────┬───────┘                                                    │
│         │                                                             │
│  ┌──────▼──────────────────┐   ┌──────────────────────────────┐      │
│  │ telegram-auth-gateway   │   │          PostgreSQL 17        │      │
│  │ Fastify + GramJS :8091  │   │  Primary datastore + Flyway   │      │
│  │ Auth + Msg Collection   │   └──────────────────────────────┘      │
│  └─────────────────────────┘                                         │
└─────────────────────────────────────────────────────────────────────┘
```

### Key Design Principles

- **Single-origin production** — frontend nginx proxies all backend paths, eliminating CORS
- **Unified ingestion** — all platforms map to `InboundIntegrationEvent` → `IntegrationIngestionService` → persisted message
- **Security filter chain** — three-layer filter pipeline: `WebhookSignatureFilter` → `InternalTokenFilter` → `JwtAuthenticationFilter`
- **Forward-only migrations** — Flyway migrations are append-only; no destructive changes

For full architecture details, see the [Architecture Highlights](#architecture-highlights) section below.

---

## Project Structure

This is a **multi-component monorepo** with three deployable services:

```
CentrAnalytics/
├── src/main/java/com/ca/centranalytics/   # Spring Boot backend
│   ├── auth/                              # JWT authentication & user management
│   ├── common/                            # Shared configuration (CORS, etc.)
│   ├── integration/                       # Core integration engine (largest module)
│   │   ├── admin/                         # Admin REST endpoints
│   │   ├── api/                           # Public API (conversations, messages, metrics)
│   │   ├── channel/                       # Platform-specific channel implementations
│   │   │   ├── vk/                        #   VK: auto-collection, discovery, group mgmt
│   │   │   ├── telegram/                  #   Telegram: gateway client, ingestion mapping
│   │   │   ├── max/wappi/                 #   Max: webhook ingestion
│   │   │   └── whatsapp/wappi/            #   WhatsApp/Wappi: webhook ingestion
│   │   ├── config/                        # Integration properties, Jackson config
│   │   ├── domain/                        # JPA entities, repositories, projections
│   │   └── ingestion/                     # Unified ingestion pipeline
│   └── user/                              # User domain (entities, controllers)
├── frontend/                              # React 19 + Vite + TypeScript frontend
│   ├── src/features/                      # Feature modules (auth, dashboard, integrations, etc.)
│   └── nginx.conf                         # Production nginx proxy config
├── telegram-auth-gateway/                 # Telegram auth & collection gateway
│   ├── src/                               # Fastify + GramJS (MTProto) server
│   └── package.json                       # Node.js dependencies
├── compose.yaml                           # Local development stack
├── compose.prod.yaml                      # Production stack (5 services)
├── Dockerfile                             # Backend multi-stage build (Java 26)
└── .github/workflows/ci-cd.yml            # CI/CD pipeline
```

---

## Building and Running

### Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 26 | Verify with `java -version` |
| Node.js | 22 | For frontend and gateway subprojects |
| Docker & Compose | Latest | Local PostgreSQL, Redis, and full-stack runs |
| Maven | Bundled | Use `./mvnw` wrapper |

### Backend Commands

```bash
# Start app locally (requires PostgreSQL)
./mvnw spring-boot:run

# Run full test suite (39 test files)
./mvnw test

# Build runnable JAR
./mvnw clean package
```

### Full Stack (Docker Compose)

```bash
# Local development (PostgreSQL + Redis + app)
docker compose up --build

# Production stack (5 services)
docker compose -f compose.prod.yaml up --build
```

### Frontend Development

```bash
cd frontend
npm install
npm run dev          # Vite dev server
npm run build        # Production build
npm test             # Vitest test suite
```

### Telegram Auth Gateway Development

```bash
cd telegram-auth-gateway
npm install
npm run dev          # Fastify dev server with tsx watch
npm run build        # TypeScript compilation
npm test             # Vitest test suite
```

---

## Configuration

Copy `.env.example` to `.env` and configure required secrets:

```bash
cp .env.example .env
```

### Essential Variables

| Variable | Purpose | Required |
|----------|---------|----------|
| `JWT_SECRET` | Base64-encoded signing secret (min 64 bytes for HS512) | Yes |
| `POSTGRES_*` | Database credentials | Yes |
| `TELEGRAM_API_ID` / `TELEGRAM_API_HASH` | Telegram TDLib credentials | For Telegram |
| `TELEGRAM_AUTH_GATEWAY_BASE_URL` | Gateway service URL | For Telegram |
| `TELEGRAM_GATEWAY_INGESTION_INTERNAL_TOKEN` | Service-to-service auth token | For Telegram |
| `VK_ACCESS_TOKEN` | VK API access | For VK |
| `VK_AUTO_COLLECTION_ENABLED` | Enable scheduled VK collection | For VK |
| `WAPPI_WEBHOOK_PATH` | WhatsApp/Max webhook path | For Wappi |

Run `openssl rand -base64 64` to generate a `JWT_SECRET`. Run `openssl rand -hex 32` for `TELEGRAM_GATEWAY_INGESTION_INTERNAL_TOKEN`.

---

## Architecture Highlights

### Ingestion Pipeline

```
Platform Webhook / Scheduler
         │
         ▼
┌─────────────────────────┐
│  Platform Controller     │  (VK, Telegram, Wappi, Max)
│  Inbound Event Mapper    │  → InboundIntegrationEvent
└──────────┬──────────────┘
           ▼
┌─────────────────────────┐
│ IntegrationIngestionService │
│  • Resolve conversation     │
│  • Resolve external user    │
│  • Deduplication            │
└──────────┬──────────────┘
           ▼
┌─────────────────────────┐
│ MessagePersistenceService │
│  • Message + attachments  │
│  • Binary content         │
└──────────┬──────────────┘
           ▼
  MessageCapturedEvent (Spring event for downstream processing)
```

### Security Filter Chain

The backend uses a three-filter security chain (ordered):

1. **`WebhookSignatureFilter`** — validates incoming webhook signatures (platform webhooks)
2. **`InternalTokenFilter`** — validates `X-Internal-Token` header (service-to-service calls)
3. **`JwtAuthenticationFilter`** — validates JWT bearer tokens (user-facing API)

Public endpoints (no auth): `/auth/register`, `/auth/login`, `/api/integrations/webhooks/**`, `/actuator/health/**`, `/swagger-ui/**`, `/api-docs/**`.

### Telegram Integration

Two modes are supported:

- **Auth Gateway mode** (production default) — external `telegram-auth-gateway` (Fastify + GramJS) handles MTProto authorization and message collection, pushing events to backend via internal ingestion endpoint
- **Direct TDLib mode** — built-in Telegram user client (legacy)

The gateway exposes `POST /api/internal/integrations/telegram-user/events` protected by `X-Internal-Token`.

### VK Auto Collection

Scheduled region-based collection without webhooks:

- Discovers VK groups by configured region
- Collects recent wall posts and comments
- Normalizes into shared integration messages feed
- Controlled by `VK_AUTO_COLLECTION_*` properties (default 15 min interval)

### Database Migrations (Flyway)

11 migrations in `src/main/resources/db/migration/`:

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

## CI/CD

GitHub Actions workflow for single-developer flow (`.github/workflows/ci-cd.yml`):

### Path-Based Selective Builds

| Path | Triggered Jobs |
|------|----------------|
| `src/**`, `pom.xml`, `Dockerfile`, `mvnw`, `.mvn/**` | Backend test + build |
| `frontend/**` | Frontend test + build |
| `telegram-auth-gateway/**` | Gateway test + build |
| `compose.prod.yaml`, `.env.example`, `README.md` | Deploy-only flag |

### Pipeline Stages

1. **PRs to `main`** — selective checks for changed areas (no deployment)
2. **Pushes to `main`** — test → build → push to `ghcr.io` → deploy changed services

### Deploy

Runs on a **self-hosted runner** on the target server:

- Pulls only changed images from GHCR
- Performs rolling restart of affected services
- Validates frontend and backend health endpoints
- Retries image pulls up to 3 times with 10s backoff

### Required GitHub Secrets

| Secret | Description |
|--------|-------------|
| `DEPLOY_PATH` | Server directory for deployment (e.g. `/opt/centranalytics`) |
| `GHCR_USERNAME` | GHCR username (usually `${{ github.actor }}`) |
| `GHCR_TOKEN` | Personal access token with `read:packages` scope |
| `FRONTEND_HEALTHCHECK_URL` | Optional, defaults to `http://127.0.0.1:18080/` |
| `BACKEND_HEALTHCHECK_URL` | Optional, defaults to container IP + `/actuator/health` |

---

## Production Topology

### Services

| Service | Image | Port | Description |
|---------|-------|------|-------------|
| `postgres` | `postgres:17` | internal | Primary datastore with persistent volume |
| `app` | `ghcr.io/.../centranalytics` | internal (exposed via nginx) | Spring Boot backend |
| `frontend` | `ghcr.io/.../centranalytics-frontend` | `18080` (configurable) | React SPA with nginx proxy |
| `centranalytics-tgproxy` | `ghcr.io/xtls/xray-core:latest` | internal | Xray egress proxy for Telegram MTProto |
| `telegram-auth-gateway` | `ghcr.io/.../centranalytics-telegram-auth-gateway` | internal (exposed to app) | Fastify + GramJS auth and collection |

### Frontend Nginx Proxy

All browser traffic goes through `frontend` nginx on a **single origin**, proxying:

- `/auth` → backend
- `/api` → backend
- `/actuator` → backend
- `/swagger-ui` → backend
- `/api-docs` → backend
- `/v3/api-docs` → backend

### Tailscale Funnel

Public webhook traffic is published through Tailscale Funnel:

- Local target: `http://127.0.0.1:18080`
- Public host: `https://debian.tail9e3c2c.ts.net`
- Configured via `tailscale funnel --bg --yes 18080`

### Health Checks

Redis actuator health is **disabled** in production configuration since the production compose stack does not include Redis. `/actuator/health` reflects actual production dependencies only.

### Telegram Proxy Management

`centranalytics-tgproxy` must be managed via `docker compose` only — do not run ad-hoc `xray` or `happ` processes. The proxy config lives at `${TELEGRAM_PROXY_CONFIG_PATH}` inside the deploy directory.

---

## Development Conventions

| Aspect | Convention |
|--------|------------|
| Indentation | 4 spaces |
| Class names | `PascalCase` |
| Methods/fields | `camelCase` |
| Constants | `UPPER_SNAKE_CASE` |
| Controller suffix | `*Controller` |
| Service suffix | `*Service` |
| Repository suffix | `*Repository` |
| DTOs | `*Request` / `*Response` |
| Dependency injection | Constructor injection via `@RequiredArgsConstructor` |
| Package structure | Feature-oriented under `com.ca.centranalytics.<domain>` |
| Commit style | Imperative prefix — `feat:`, `fix:`, `chore:` |
| PR guidelines | Explain change, note config/migration impact, link issue, include request/response samples for API changes |

---

## Security Notes

- **Never commit secrets** — use `.env.example` as template; copy to `.env` or shell environment
- **Internal endpoints** (`/api/internal/*`) are protected by `X-Internal-Token` header validation
- **Flyway migrations** are forward-only; review carefully before adding
- **Auth and webhook endpoints** are externally exposed — review `SecurityConfig` changes carefully
- **tgproxy** must be managed via `docker compose`, not ad-hoc processes
- **JWT secret** must be at least 64 bytes (HS512)

---

## License

Proprietary — CentrAnalytics © 2025
