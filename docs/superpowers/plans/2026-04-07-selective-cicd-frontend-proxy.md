# Selective CI/CD With Frontend Proxy Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship backend and frontend as separate containers with selective CI/CD and frontend-side nginx proxying in production.

**Architecture:** Keep backend and frontend as distinct images. Use the frontend nginx container as the single public origin and proxy backend routes over the Docker network. Make GitHub Actions detect changed paths so only relevant tests, builds, and deploy steps run.

**Tech Stack:** GitHub Actions, GHCR, Docker Compose, nginx, Vite, Spring Boot

---

### Task 1: Add frontend production container

**Files:**
- Create: `frontend/Dockerfile`
- Create: `frontend/nginx.conf`

- [ ] Add a multi-stage frontend Docker build.
- [ ] Add nginx config for SPA hosting and backend proxying.

### Task 2: Update production runtime files

**Files:**
- Modify: `compose.prod.yaml`
- Modify: `.env.example`
- Modify: `README.md`

- [ ] Add a `frontend` service to production compose.
- [ ] Make frontend the public port-binding service.
- [ ] Document deploy/runtime expectations for `.env` and `.images.env`.

### Task 3: Refactor workflow for selective service delivery

**Files:**
- Modify: `.github/workflows/ci-cd.yml`

- [ ] Detect backend/frontend/deploy path changes.
- [ ] Run tests only for changed service areas.
- [ ] Build and push only changed images.
- [ ] Deploy only changed services while preserving current image refs.

### Task 4: Verify locally

**Files:**
- Modify: `frontend/package-lock.json`

- [ ] Run `npm test` in `frontend/`.
- [ ] Run `npm run build` in `frontend/`.
- [ ] Re-read workflow and compose files for consistency.
