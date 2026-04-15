# Repository Guidelines

## Project Structure & Module Organization

This is a **multi-component monorepo** built around a Maven-based Spring Boot 4.0.5 backend (Java 26) with two additional deployable subprojects.

### Backend (`src/main/java/com/ca/centranalytics`)

Application code is grouped by feature area under `com.ca.centranalytics`:

| Package | Responsibility |
|---------|----------------|
| `auth/` | JWT authentication — controllers, security filters, services, DTOs |
| `common/` | Shared configuration (CORS properties, etc.) |
| `integration/` | Core integration engine — the largest module covering platform channels, domain, and ingestion |
| `user/` | User management — entities, repositories, controllers |

Configuration and database migrations live in `src/main/resources`, with Flyway scripts in `src/main/resources/db/migration` (11 migrations as of V11).

### Frontend (`frontend/`)

React 19 + Vite + TypeScript SPA. Sources under `frontend/src/` with feature modules in `src/features/`. Production nginx proxy config in `frontend/nginx.conf`.

### Telegram Auth Gateway (`telegram-auth-gateway/`)

Fastify + GramJS (MTProto) service for Telegram user authorization and message collection. Sources under `telegram-auth-gateway/src/`.

### Docker & Configuration

Local runtime assets and container wiring are defined in `compose.yaml`, `compose.prod.yaml`, `Dockerfile`, and `.env.example`. The production stack runs 5 services: `postgres`, `app`, `frontend`, `centranalytics-tgproxy`, and `telegram-auth-gateway`.

## Build, Test, and Development Commands

Use the Maven wrapper so the project runs with the repo's expected tooling. This project targets **Java 26**, so verify `java -version` before building.

### Backend

```bash
./mvnw spring-boot:run    # starts the app locally
./mvnw test               # runs the JUnit 5 test suite (39 test files)
./mvnw clean package      # builds the runnable JAR
```

### Frontend

```bash
cd frontend && npm ci
npm run dev               # Vite dev server
npm run build             # production build
npm test                  # Vitest test suite
```

### Telegram Auth Gateway

```bash
cd telegram-auth-gateway && npm ci
npm run dev               # Fastify dev server with tsx watch
npm run build             # TypeScript compilation
npm test                  # Vitest test suite
```

### Docker Compose

```bash
docker compose up --build                           # local stack (PostgreSQL + Redis + app)
docker compose -f compose.prod.yaml up --build      # production stack (5 services)
```

Flyway migrations in `src/main/resources/db/migration` are applied automatically on application startup.

## Coding Style & Naming Conventions

Follow standard Java conventions with 4-space indentation and one top-level class per file. Keep packages feature-oriented under `com.ca.centranalytics.<domain>`. Class names use `PascalCase`; methods and fields use `camelCase`; constants use `UPPER_SNAKE_CASE`. Controllers end with `Controller`, services with `Service`, repositories with `Repository`, and DTOs with `Request` or `Response`. Prefer constructor injection; the codebase uses Lombok annotations such as `@RequiredArgsConstructor`.

Frontend and gateway subprojects follow standard TypeScript/JavaScript conventions with 2-space indentation.

## Testing Guidelines

Tests use JUnit 5, Spring Boot test support, MockMvc, and Testcontainers for integration coverage. The backend test suite includes 39 test files covering auth, integration channels (VK, Telegram, Wappi), ingestion pipeline, domain persistence, and configuration.

Add tests under the matching package in `src/test/java`. Name unit tests `*Test` and broader Spring or container-backed tests `*IntegrationTest`, following existing examples like `AuthControllerIntegrationTest`. Run `./mvnw test` before opening a PR, and add coverage for new endpoints, migrations, and security-sensitive paths.

Frontend tests use Vitest + Testing Library (`*.test.tsx`). Gateway tests use Vitest (`*.test.ts`). Place tests alongside the files they test.

## Commit & Pull Request Guidelines

Recent history follows concise, imperative commits with prefixes such as `feat:`, `fix:`, and `chore:`. Keep commits focused and describe the user-visible change, for example `feat: add telegram session bootstrap`. PRs should explain the change, note any config or migration impact, link the related issue, and include request/response samples for API changes when useful.

When changing a subproject, note which service is affected (backend, frontend, or telegram-auth-gateway) so reviewers understand the CI/CD impact.

## Security & Configuration Tips

Do not commit real secrets. Copy values from `.env.example` into a local `.env` or your shell environment, especially `JWT_SECRET`, `TELEGRAM_API_ID`/`TELEGRAM_API_HASH`, and Telegram gateway credentials. The backend uses a **three-filter security chain** (in order):

1. **`WebhookSignatureFilter`** — validates platform webhook signatures
2. **`InternalTokenFilter`** — validates `X-Internal-Token` for service-to-service calls
3. **`JwtAuthenticationFilter`** — validates JWT bearer tokens for user-facing API

Keep Flyway migrations forward-only and review any auth or webhook changes carefully because they affect exposed endpoints. The `centranalytics-tgproxy` (Xray) must be managed via `docker compose`, not ad-hoc processes. Internal ingestion endpoints (`/api/internal/*`) must always be protected by `X-Internal-Token` validation.
