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
