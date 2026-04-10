# Overview Analytics Design

## Goal

Replace the current `Обзор` placeholder with a real operational analytics screen that gives operators a fast, trustworthy picture of platform activity and ingestion health.

The new overview page should:

- present a single top-level cockpit for the whole system
- show separate platform slices for `Telegram`, `VK`, `Whatsapp`, and `Max`
- combine traffic analytics, integration health, and attention signals on one screen
- support time-based analysis for `24h`, `7d`, and `30d`

This change should fix two current problems:

- `Обзор` is only a navigation placeholder and provides no actionable information
- operators must drill into platform-specific pages to understand whether ingestion is healthy and where traffic is coming from

## Approved Product Direction

`Обзор` is a dedicated analytics page, not a variant of the existing messages table.

The current `DashboardPage` remains the platform-specific `Сообщения` screen and continues to serve detailed message browsing for a selected platform.

The new overview screen becomes the system entry point after authentication and should answer three questions immediately:

- what is happening across the system right now
- which platforms are active, degraded, or inactive
- where the operator needs to pay attention first

## Page Structure

The page should be organized into four layers.

### 1. Overview Header

The top area contains:

- page title and short description
- `generatedAt` or equivalent "updated at" timestamp
- a period switcher for `24h`, `7d`, and `30d`
- a manual refresh action

Behavior rules:

- the selected period applies to the whole page
- refresh reloads the overview without leaving the page
- the current selection must remain visible while reloading

### 2. Global Summary Row

Below the header, render a compact system-level KPI row with:

- total messages
- active conversations
- active authors
- platforms with issues

This summary is intended for immediate orientation and should remain short and legible.

### 3. Platform Sections

Render one section for each platform:

- `Telegram`
- `VK`
- `Whatsapp`
- `Max`

Each section contains:

- `3-4` KPI highlights
- a compact trend visualization for the selected period
- an integration status panel
- a short attention list

Platforms should always render in the overview even if there is no data. A platform without traffic or setup should show a clear `inactive` or `not configured` state rather than disappearing.

### 4. Optional Recent System Activity

If a clean backend source exists, the page may include a final compact block for recent system changes or events. This block is optional and should be treated as deferrable if there is no reliable source yet.

## Platform Section Content

Each platform section should combine analytics and operations, not just one of them.

### KPI Highlights

Each platform section should expose a small set of top metrics, such as:

- `messageCount`
- `conversationCount`
- `activeAuthorCount`
- `shareOfTraffic` or `avgMessagesPerConversation`

The final KPI choice may vary slightly per platform, but the cards should remain visually consistent.

### Trend

Each platform section should include a compact trend strip for message volume over the selected window.

For the first iteration, the trend only needs:

- time bucket
- message count

The backend should return normalized time buckets. The frontend should not derive a trend by sampling raw message lists.

### Integration Status

Each platform section should include a compact operational state card describing channel health.

Core fields:

- `syncStatus`
- `lastEventAt`
- `lastSuccessAt`
- `lastErrorMessage`
- `sourceCount`

Platform-specific examples:

- Telegram: session state, authentication wait state, recent inbound activity
- Whatsapp: webhook health, source readiness, recent successful delivery
- VK: group/source coverage, recent collection activity, collection health
- Max: source readiness and last known activity, if available

### Attention Items

Each section should expose a short prioritized list of operator-facing issues or observations, for example:

- no events for the last 6 hours
- Telegram session is waiting for password
- webhook is inactive
- sources are not configured

This list is the main "what needs attention" output of the section. It should be backend-generated and already prioritized for display.

## Status Model

Each platform section should expose one normalized state:

- `healthy`
- `warning`
- `critical`
- `inactive`

Rules:

- the backend computes these statuses
- the frontend only renders them
- a missing or unreliable signal should result in `inactive` or `unknown`-like messaging inside the section, not invented warning logic in the UI

The page must tolerate partial failures:

- if one platform section fails, the rest of the overview still renders
- a degraded platform should show an explicit error state inside its section
- a global fatal state should be reserved for total endpoint failure only

## Backend API Contract

Introduce a dedicated endpoint for the overview, for example:

- `GET /api/overview?window=24h|7d|30d`

Recommended response shape:

- `generatedAt`
- `window`
- `summary`
- `platforms`

### Summary

The `summary` block should provide:

- `messageCount`
- `conversationCount`
- `activeAuthorCount`
- `platformIssueCount`

### Platforms

The `platforms` block should include entries for:

- `TELEGRAM`
- `VK`
- `WHATSAPP`
- `MAX`

Each platform entry should include:

- `platform`
- `label`
- `status`
- `highlights`
- `trend`
- `integration`
- `attentionItems`

The backend should return values already normalized for display. The frontend should not be responsible for deriving business status, issue severity, or trend bucket logic from low-level records.

## Backend Architecture

Add a dedicated overview query path instead of extending `/api/messages`.

Recommended structure:

- `OverviewController`
- `OverviewQueryService`
- focused query/repository methods for summary and platform aggregates
- DTOs specific to overview rendering

Implementation rules:

- do not build overview analytics by loading large raw message lists into memory
- aggregate in repository/query code wherever practical
- keep status classification logic in the service layer
- keep platform-specific health assembly isolated enough that new platform rules can be added without destabilizing the whole endpoint

## Frontend Architecture

Replace the current `overview` placeholder in `frontend/src/app/App.tsx` with a dedicated `OverviewPage`.

Recommended frontend pieces:

- `OverviewPage.tsx`
- `overview.api.ts`
- `overview.types.ts`

Rules:

- `OverviewPage` owns the selected period state
- it fetches one overview payload per selected period
- it renders the global summary row plus platform sections
- it does not reuse the table-oriented `DashboardPage`
- platform message browsing remains in the existing platform `Сообщения` screens

## Visual Direction

The overview page should feel like an editorial operational dashboard rather than a CRUD screen.

Direction:

- preserve the project's existing light atmospheric visual language
- use strong sectioning, contrast, and spacing to distinguish global summary from platform slices
- keep KPI cards compact and scannable
- make the trend strip visually supportive rather than chart-heavy
- make status and attention items immediately legible without requiring expansion

The visual emphasis should be:

- first on current state
- second on traffic dynamics
- third on supporting diagnostics

## Error Handling

The page should support three levels of error handling.

### Full Endpoint Failure

If the overview request fails entirely:

- show a page-level error
- preserve the selected period control
- allow retry through manual refresh

### Partial Platform Failure

If only one platform slice cannot be assembled:

- render the section shell
- show a degraded-state message inside that platform
- continue rendering other platforms

### Empty or Inactive State

If a platform has no activity or is not configured:

- show the platform section
- render zero or empty metrics safely
- explain that the platform is inactive or not configured

## Testing

### Backend

Add tests for:

- summary aggregation for each time window
- per-platform status classification
- integration of platform-specific health signals
- the overview endpoint contract

### Frontend

Add tests for:

- rendering the overview page instead of the old placeholder
- switching time windows and reloading data
- rendering all platform sections
- degraded platform section handling
- inactive platform rendering
- page-level error handling

## Out Of Scope

This design does not require:

- replacing the platform-specific messages table
- adding complex charting libraries if simple trend rendering is sufficient
- inventing synthetic health signals where the backend has no reliable data
- implementing a recent system activity feed if there is no clean source
