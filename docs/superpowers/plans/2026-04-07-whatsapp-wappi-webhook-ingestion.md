# WhatsApp Wappi Webhook Ingestion Implementation Plan

## Goal

Use Wappi webhooks as the inbound WhatsApp transport for this deployment and document the operational setup.

## Plan

1. Expose an explicit Wappi webhook path in application configuration and environment examples.
2. Document the production webhook URL and the required Wappi admin settings.
3. Document Tailscale Funnel as the public ingress layer for the private server.
4. Record the operational command that enables Funnel in background mode.
5. Remove obsolete polling configuration and runtime components so Wappi stays webhook-only.

## Operational Notes

- public webhook URL: `https://debian.tail9e3c2c.ts.net/api/integrations/webhooks/wappi`
- local app listener used by Funnel: `127.0.0.1:18080`
- required Wappi webhook type: `Все входящие сообщения и файлы`
- recommended Tailscale commands:

```bash
sudo tailscale set --operator=deployer
tailscale funnel --bg --yes 18080
```

## Superseded Decision

The earlier pull-based design remains in the repository as historical context, but the active deployment decision is webhook-based because Tailscale Funnel provides a stable public HTTPS entrypoint for Wappi.
