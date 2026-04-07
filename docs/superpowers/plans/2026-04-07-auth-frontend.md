# Auth Frontend Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone frontend auth screen with login and registration modes, connected to the existing backend auth API.

**Architecture:** Keep the backend as the auth authority and add a separate `frontend/` app inside the repository. Add minimal backend CORS support for browser-based local development, then implement the auth UI with a feature-oriented frontend structure.

**Tech Stack:** Spring Boot, JUnit 5, React, Vite, TypeScript, Vitest, React Testing Library

---

### Task 1: Backend CORS support

**Files:**
- Modify: `src/main/java/com/ca/centranalytics/auth/security/SecurityConfig.java`
- Test: `src/test/java/com/ca/centranalytics/auth/controller/AuthControllerIntegrationTest.java`

- [ ] Add a failing integration test for auth preflight from a frontend origin.
- [ ] Run the focused backend test and confirm it fails for the expected reason.
- [ ] Add minimal CORS configuration in security.
- [ ] Re-run the focused backend test and confirm it passes.

### Task 2: Frontend app scaffold

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tsconfig.node.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/index.html`
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/app/App.tsx`
- Create: `frontend/src/app/App.test.tsx`
- Create: `frontend/src/app/test/setup.ts`
- Create: `frontend/src/shared/styles/global.css`

- [ ] Write a failing frontend render test for the auth screen shell.
- [ ] Run the focused frontend test and confirm it fails correctly.
- [ ] Create the minimal Vite + React scaffold to make the test pass.
- [ ] Re-run the focused frontend test and confirm it passes.

### Task 3: Auth behavior and styling

**Files:**
- Create: `frontend/src/features/auth/AuthPage.tsx`
- Create: `frontend/src/features/auth/auth.api.ts`
- Create: `frontend/src/features/auth/auth.types.ts`
- Create: `frontend/src/features/auth/AuthPage.test.tsx`
- Modify: `frontend/src/app/App.tsx`
- Modify: `frontend/src/shared/styles/global.css`

- [ ] Add failing tests for mode switching, client validation, and backend error rendering.
- [ ] Run the focused frontend tests and confirm they fail.
- [ ] Implement the auth page with real API integration and the approved `Soft glass console` visual direction.
- [ ] Re-run the focused frontend tests and confirm they pass.

### Task 4: Verification

**Files:**
- Modify: `.gitignore`
- Create: `frontend/.env.example`

- [ ] Ignore frontend install artifacts where needed.
- [ ] Add frontend environment documentation for backend base URL.
- [ ] Run focused backend tests.
- [ ] Run focused frontend tests.
- [ ] Run the frontend production build.
