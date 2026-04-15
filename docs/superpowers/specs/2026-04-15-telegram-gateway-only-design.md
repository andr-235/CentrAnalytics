# Telegram Gateway-Only Design

## Goal

Remove the legacy TDLib-based Telegram user-session runtime from CentrAnalytics and make `telegram-auth-gateway` the only source of truth for Telegram auth, status, and message ingestion.

## Scope

- Remove TDLib runtime classes, persistence, and bootstrap from backend.
- Stop overview and platform status from reading `telegram_user_session`.
- Keep Telegram ingestion only through the internal gateway endpoint.
- Keep Telegram admin API only as a gateway-backed auth/status surface.
- Update frontend Telegram page to reflect gateway-backed state only.

## Decisions

### Backend

- Delete TDLib runtime classes and related mapper/session code.
- Remove `telegram_user_session` usage from overview/status services.
- Telegram platform health will be derived from recent Telegram message activity, integration source health, and gateway-backed current session availability where needed by admin UI.

### Frontend

- Telegram page remains an auth/session operations page.
- `lastSyncAt` is removed from the Telegram session contract because gateway does not provide it.
- UI messaging should no longer imply local TDLib sync state.

### Data

- `telegram_user_session` becomes legacy and is dropped by migration.
- Existing TDLib filesystem state is no longer part of runtime behavior.

## Verification

- Backend tests for overview, security, Telegram admin API, and gateway ingestion pass.
- Frontend tests for Telegram session page pass.
- Full backend Maven test suite passes.
