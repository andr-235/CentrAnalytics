# Selective CI/CD With Frontend Proxy Design

**Goal:** Extend the existing delivery pipeline so backend and frontend build, ship, and deploy as separate containers while avoiding unnecessary rebuilds when only one side changes.

## Context

The repository already has:

- Spring Boot backend image build and deploy
- `compose.prod.yaml` for production runtime
- GitHub Actions workflow for test, build, and deploy

The repository now also includes a standalone frontend in `frontend/`.

## Architecture

Production will run three services:

- `postgres`
- `app`
- `frontend`

The `frontend` container is the public entrypoint. It serves the built Vite assets via `nginx` and proxies application paths to the backend container on the internal Docker network.

Proxied backend paths:

- `/auth`
- `/api`
- `/actuator`
- `/swagger-ui`
- `/swagger-ui.html`
- `/api-docs`
- `/v3/api-docs`

This keeps production on a single origin and avoids browser-side CORS concerns.

## CI/CD

The GitHub Actions workflow must become selective:

- backend tests/builds run only when backend files change
- frontend tests/builds run only when frontend files change
- deploy runs when backend, frontend, or deploy configuration changes
- deploy updates only the changed services

Two images are published to GHCR:

- `ghcr.io/<owner>/centranalytics`
- `ghcr.io/<owner>/centranalytics-frontend`

## Deploy Model

Deploy runs on the existing self-hosted runner.

The deploy directory keeps:

- `.env` for runtime secrets
- `.images.env` for the currently deployed image refs
- `compose.prod.yaml`

When a service is rebuilt, the deploy job updates the corresponding image ref in `.images.env` and restarts only that service through Docker Compose.

## Verification

- frontend: `npm test`
- frontend: `npm run build`
- workflow YAML validation by inspection
- production health checks:
  - frontend root `/`
  - proxied backend `/actuator/health`
