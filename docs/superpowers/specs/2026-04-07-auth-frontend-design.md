# Auth Frontend Design

**Goal:** Build the first frontend screen for CentrAnalytics as a standalone app inside this repository: a polished authentication page with a mode switcher for login and registration, wired to the existing backend auth API.

## Context

The repository is currently a Spring Boot backend. Authentication already exists on the server with:

- `POST /auth/login`
- `POST /auth/register`

Both endpoints accept:

```json
{
  "username": "string",
  "password": "string"
}
```

Both return:

```json
{
  "token": "jwt"
}
```

Validation and authentication errors return either:

- field maps, for example `{ "username": "..." }`
- generic error payloads, for example `{ "error": "Invalid username or password" }`

## Product Scope

This iteration only includes the auth screen. No Google login, Apple login, password reset, profile setup, or dashboard routing is included.

## UX Direction

The page follows a `Soft glass console` direction inspired by `docs/pages_design/auth.png` but not copied directly.

Key traits:

- bright atmospheric background with blurred color fields
- central frosted panel with stronger geometry and premium spacing
- single screen with a segmented mode switcher: `Вход` and `Регистрация`
- only two fields: `username` and `password`
- clear inline validation, backend error banner, and loading state

## Architecture

The frontend will live in `frontend/` as a separate React + Vite + TypeScript app inside the same repository.

Initial structure:

- `frontend/src/app` for app shell
- `frontend/src/features/auth` for auth page, form logic, and API calls
- `frontend/src/shared` for shared styles and helpers

The backend remains the source of truth for authentication. The frontend stores the JWT token locally after a successful response.

## Backend Support

Because the frontend runs on a separate local dev server, backend CORS support is required for browser requests from the frontend origin.

This iteration will add a minimal CORS configuration that allows configured frontend origins to call the backend auth endpoints and other API routes.

## Error Handling

- Client validation prevents obviously invalid submissions.
- Backend field validation maps to inline field errors.
- Backend generic errors map to a top-level alert region.
- Network failures map to a generic user-facing message.

## Verification

- backend integration test for CORS preflight
- frontend tests for mode switching, validation, and API error rendering
- production build for the frontend
