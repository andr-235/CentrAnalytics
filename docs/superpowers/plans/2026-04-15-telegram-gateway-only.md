# Telegram Gateway-Only Implementation Plan

1. Remove TDLib classes, persistence, and tests from backend.
2. Refactor overview/platform status to stop depending on Telegram TDLib session state.
3. Simplify Telegram session API model to gateway-backed fields only.
4. Update frontend Telegram page and tests to match the new session contract.
5. Add/drop migrations and run focused tests, then full backend verification.
