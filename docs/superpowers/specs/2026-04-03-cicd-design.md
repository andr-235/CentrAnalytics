# CI/CD Design For Single-Developer Deployment

## Goal

Set up a simple and reliable delivery path for `CentrAnalytics` using GitHub Actions, GHCR, and one SSH-accessible server running Docker Compose.

## Recommended Approach

Use one GitHub Actions workflow with three stages:

1. `test` on pull requests and pushes to `main`
2. `build_and_push` on pushes to `main` to publish a versioned image to `ghcr.io`
3. `deploy` on pushes to `main` to update the server over SSH with `docker compose`

## Runtime Design

- Production server keeps runtime secrets in `.env`
- GitHub Actions keeps only deployment access secrets
- `compose.prod.yaml` uses `APP_IMAGE` injected by the workflow, so deploys are tied to a specific image digest/tag
- Health validation uses `/actuator/health`

## Why This Design

- Minimal operational overhead for one developer
- Clear rollback point by re-deploying a previous image tag
- No need for Kubernetes or a dedicated CD system
- Keeps secrets off the repository and avoids rebuilding on the server
