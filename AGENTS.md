# Repository Guidelines

## Project Structure & Module Organization
This repository is a Maven-based Spring Boot service. Application code lives under `src/main/java/com/ca/centranalytics`, grouped by feature areas such as `auth`, `integration`, and `user`. Configuration and database migrations live in `src/main/resources`, with Flyway scripts in `src/main/resources/db/migration`. Tests mirror the main package structure under `src/test/java/com/ca/centranalytics`. Local runtime assets and container wiring are defined in `compose.yaml`, `Dockerfile`, and `.env.example`.

## Build, Test, and Development Commands
Use the Maven wrapper so the project runs with the repo’s expected tooling. This project targets `Java 26`, so verify `java -version` before building.

- `./mvnw spring-boot:run` starts the app locally.
- `./mvnw test` runs the JUnit 5 test suite.
- `./mvnw clean package` builds the runnable jar.
- `docker compose up --build` starts PostgreSQL and the app in containers.
- Flyway migrations in `src/main/resources/db/migration` are applied automatically on application startup.

## Coding Style & Naming Conventions
Follow standard Java conventions with 4-space indentation and one top-level class per file. Keep packages feature-oriented under `com.ca.centranalytics.<domain>`. Class names use `PascalCase`; methods and fields use `camelCase`; constants use `UPPER_SNAKE_CASE`. Controllers end with `Controller`, services with `Service`, repositories with `Repository`, and DTOs with `Request` or `Response`. Prefer constructor injection; the codebase currently uses Lombok annotations such as `@RequiredArgsConstructor`.

## Testing Guidelines
Tests use JUnit 5, Spring Boot test support, MockMvc, and Testcontainers for integration coverage. Add tests under the matching package in `src/test/java`. Name unit tests `*Test` and broader Spring or container-backed tests `*IntegrationTest`, following existing examples like `AuthControllerIntegrationTest`. Run `./mvnw test` before opening a PR, and add coverage for new endpoints, migrations, and security-sensitive paths.

## Commit & Pull Request Guidelines
Recent history follows concise, imperative commits with prefixes such as `feat:`, `fix:`, and `chore:`. Keep commits focused and describe the user-visible change, for example `feat: add telegram session bootstrap`. PRs should explain the change, note any config or migration impact, link the related issue, and include request/response samples for API changes when useful.

## Security & Configuration Tips
Do not commit real secrets. Copy values from `.env.example` into a local `.env` or your shell environment, especially `JWT_SECRET` and Telegram credentials. Keep Flyway migrations forward-only and review any auth or webhook changes carefully because they affect exposed endpoints.
