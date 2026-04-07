# CentrAnalytics

## CI/CD

Repository is configured for a single-developer flow on GitHub Actions:

- pull requests to `main` run only the relevant checks for changed areas
- pushes to `main` run selective tests, build changed container images, push them to `ghcr.io`, and deploy only the changed services
- deployment runs on a self-hosted runner on the server, copies [compose.prod.yaml](/home/pc051/IdeaProjects/CentrAnalytics/compose.prod.yaml) into the deploy directory, and validates both the frontend root and proxied backend health

### Required GitHub secrets

- `DEPLOY_PATH`
- `GHCR_USERNAME`
- `GHCR_TOKEN`
- `FRONTEND_HEALTHCHECK_URL` optional, defaults to `http://127.0.0.1:18080/`
- `BACKEND_HEALTHCHECK_URL` optional, defaults to `http://127.0.0.1:18080/actuator/health`

`GHCR_TOKEN` should be a token that can read packages from GHCR on the target server. The workflow itself pushes images with the built-in `GITHUB_TOKEN`.

### Server bootstrap

1. Install Docker Engine and Docker Compose plugin.
2. Create the deploy directory, for example `/opt/centranalytics`.
3. Copy `.env.example` to `/opt/centranalytics/.env` and fill in real secrets.
4. Install and register a self-hosted GitHub Actions runner on the deployment server.
5. Ensure the runner user can run `docker`.
6. Create `/opt/centranalytics/.images.env` on first deploy if you want to seed image refs manually, otherwise the workflow will create it.
7. The workflow will copy [compose.prod.yaml](/home/pc051/IdeaProjects/CentrAnalytics/compose.prod.yaml) into the deploy directory on each deploy and update only the changed services.

Minimal bootstrap example:

```bash
sudo mkdir -p /opt/centranalytics
sudo chown -R "$USER":"$USER" /opt/centranalytics
cp .env.example /opt/centranalytics/.env
touch /opt/centranalytics/.images.env
```

The deployed image refs are stored in `/opt/centranalytics/.images.env`. The server-side `.env` should keep runtime secrets and port settings only.

## Production Topology

Production Docker Compose now runs:

- `postgres`
- `app`
- `frontend`

The public entrypoint is `frontend`, which serves the built Vite assets with `nginx` and proxies:

- `/auth`
- `/api`
- `/actuator`
- `/swagger-ui`
- `/api-docs`
- `/v3/api-docs`

This keeps the browser on one origin and removes the need for production CORS configuration between frontend and backend.

## Wappi Webhooks

Wappi inbound WhatsApp ingestion is wired through a public webhook endpoint:

- app endpoint: `/api/integrations/webhooks/wappi`
- public URL on the current server: `https://debian.tail9e3c2c.ts.net/api/integrations/webhooks/wappi`
- required Wappi webhook type: `Все входящие сообщения и файлы`

Recommended `.env` values for webhook mode:

```bash
WAPPI_WEBHOOK_PATH=/api/integrations/webhooks/wappi
```

Notes:

- inbound files are persisted into PostgreSQL through `message_attachment` plus `message_attachment_content`
- Wappi can send either media URLs or file content; the ingestion layer stores attachment metadata and binary content when the payload contains file bytes

## VK Auto Collection

VK ingestion now supports scheduled region-based collection without webhooks:

- the app periodically searches VK groups for the configured region
- for discovered groups it collects recent wall posts
- for recent posts it also collects comments
- collected posts and comments are normalized into the shared integration messages feed

Recommended `.env` values for scheduler mode:

```bash
VK_AUTO_COLLECTION_ENABLED=true
VK_AUTO_COLLECTION_REGION=Primorsky Krai
VK_AUTO_COLLECTION_GROUP_SEARCH_LIMIT=25
VK_AUTO_COLLECTION_POST_LIMIT=10
VK_AUTO_COLLECTION_COMMENT_POST_LIMIT=5
VK_AUTO_COLLECTION_COMMENT_LIMIT=20
VK_AUTO_COLLECTION_MODE=HYBRID
VK_AUTO_COLLECTION_FIXED_DELAY_MS=900000
```

Notes:

- `VK_AUTO_COLLECTION_REGION` is required when scheduler mode is enabled
- `VK_AUTO_COLLECTION_FIXED_DELAY_MS` controls how often the background collection runs
- auto-collection reuses the existing discovery and ingestion pipeline, so repeated runs are safe for already known posts and comments

## Tailscale Funnel

The server publishes webhook traffic through Tailscale Funnel:

- local frontend target: `http://127.0.0.1:18080`
- public host: `https://debian.tail9e3c2c.ts.net`
- current routing: `443 -> 127.0.0.1:18080`

The node was configured with:

```bash
sudo tailscale set --operator=deployer
tailscale funnel --bg --yes 18080
```

`tailscale funnel --bg` stores the Funnel configuration in the Tailscale daemon and restores it after reboot, so no separate systemd unit is required as long as the Tailscale service itself starts normally.

## Production Health

The production compose stack for CentrAnalytics does not include Redis. Because of that, Redis actuator health is disabled in application configuration so `/actuator/health` reflects the actual dependencies of this service in production instead of reporting a false `DOWN`.
