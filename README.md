# CentrAnalytics

## CI/CD

Repository is configured for a single-developer flow on GitHub Actions:

- pull requests to `main` run `./mvnw test`
- pushes to `main` run tests, build a Docker image, push it to `ghcr.io`, and deploy to your server over SSH
- deployment runs on a self-hosted runner on the server, copies [compose.prod.yaml](/home/pc051/IdeaProjects/CentrAnalytics/compose.prod.yaml) into the deploy directory, and validates `/actuator/health`

### Required GitHub secrets

- `DEPLOY_PATH`
- `GHCR_USERNAME`
- `GHCR_TOKEN`
- `HEALTHCHECK_URL` optional, defaults to `http://127.0.0.1:8080/actuator/health`

`GHCR_TOKEN` should be a token that can read packages from GHCR on the target server. The workflow itself pushes images with the built-in `GITHUB_TOKEN`.

### Server bootstrap

1. Install Docker Engine and Docker Compose plugin.
2. Create the deploy directory, for example `/opt/centranalytics`.
3. Copy `.env.example` to `/opt/centranalytics/.env` and fill in real secrets.
4. Install and register a self-hosted GitHub Actions runner on the deployment server.
5. Ensure the runner user can run `docker`.
6. The workflow will copy [compose.prod.yaml](/home/pc051/IdeaProjects/CentrAnalytics/compose.prod.yaml) into the deploy directory on each deploy and restart the stack.

Minimal bootstrap example:

```bash
sudo mkdir -p /opt/centranalytics
sudo chown -R "$USER":"$USER" /opt/centranalytics
cp .env.example /opt/centranalytics/.env
```

The deployed image is injected at runtime via `APP_IMAGE`, so the server-side `.env` should not contain an image tag.

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

## Tailscale Funnel

The server publishes webhook traffic through Tailscale Funnel:

- local app target: `http://127.0.0.1:18080`
- public host: `https://debian.tail9e3c2c.ts.net`
- current routing: `443 -> 127.0.0.1:18080`

The node was configured with:

```bash
sudo tailscale set --operator=deployer
tailscale funnel --bg --yes 18080
```

`tailscale funnel --bg` stores the Funnel configuration in the Tailscale daemon and restores it after reboot, so no separate systemd unit is required as long as the Tailscale service itself starts normally.
