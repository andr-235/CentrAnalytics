# VK Group Management Frontend Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add scrollable VK group management controls to the integrations page so operators can collect or delete one or many existing groups from the current UI.

**Architecture:** Extend the existing integrations feature only. Add typed API helpers for new VK group actions, wire local selection and mutation state into the existing VK panel, and update styles to support a scrollable list with row and batch actions.

**Tech Stack:** React, TypeScript, Testing Library, existing integrations feature API/types

---

### Task 1: Add failing frontend tests

**Files:**
- Modify: `frontend/src/features/integrations/IntegrationsPage.test.tsx`

- [ ] Add failing tests for batch selection and collect/delete actions.
- [ ] Add a failing test proving the VK list renders more than six groups inside a scrollable container.
- [ ] Run the focused integrations page test and confirm the new expectations fail.

### Task 2: Add API contracts and helpers

**Files:**
- Modify: `frontend/src/features/integrations/integrations.types.ts`
- Modify: `frontend/src/features/integrations/integrations.api.ts`

- [ ] Add typed request/response models for VK batch collect/delete operations.
- [ ] Add fetch helpers for the new backend endpoints.
- [ ] Re-run the focused integrations page test and confirm the UI still fails on missing behavior, not missing types.

### Task 3: Implement VK management UI

**Files:**
- Modify: `frontend/src/features/integrations/IntegrationsPage.tsx`
- Modify: `frontend/src/shared/styles/global.css`

- [ ] Add selection state, batch action handlers, and row action handlers.
- [ ] Remove the six-row cap and render the full VK list in a scrollable container.
- [ ] Add inline feedback and disabled states for pending mutations.
- [ ] Re-run the focused integrations page test and confirm it passes.

### Task 4: Verify

**Files:**
- None required

- [ ] Run `npm --prefix frontend test -- --runInBand frontend/src/features/integrations/IntegrationsPage.test.tsx`.
- [ ] Run the frontend feature test slice if the environment allows it.
