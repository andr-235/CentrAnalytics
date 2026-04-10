# Platform Navigation Frontend Design

## Goal

Replace the current flat sidebar with a platform-first navigation model that reflects how operators actually work:

- first choose a channel domain: `Вконтакте`, `Телеграм`, `Max`, `Whatsapp`
- then choose a channel-specific subsection such as `Сообщения`, `Группы`, `Сессия`, or `Webhook`
- keep `Настройки` as a global system section, not tied to any single platform

The change should fix two current problems:

- the sidebar is organized around generic entities instead of platforms
- the frontend shell does not provide a clean structure for channel-specific screens

## Approved Information Architecture

Top-level navigation:

- `Обзор`
- `Вконтакте`
  - `Сообщения`
  - `Группы`
  - `Сбор`
- `Телеграм`
  - `Сообщения`
  - `Диалоги`
  - `Сессия`
- `Max`
  - `Сообщения`
  - `Источники`
- `Whatsapp`
  - `Сообщения`
  - `Webhook`
  - `Источники`
- `Настройки`

Rules:

- `Обзор` and `Настройки` are single pages without nested items
- platform blocks are expandable and contain only platform-specific subsections
- `Настройки` stay global and must not be duplicated inside platform blocks

## UX Direction

The sidebar should behave as a stack of large accordion panels rather than a list of peer pages.

Each platform block should show:

- a large platform title
- a compact status line
- a chevron or equivalent disclosure indicator

Interaction rules:

- clicking a platform header only expands or collapses the block
- expanding a block does not automatically change the page content
- content on the right changes only when a second-level item is selected
- only one platform block is expanded at a time
- if a second-level item is selected, the parent platform must remain visibly active
- previously selected subsection for a platform should be preserved when the operator returns to it

Visual direction:

- large, deliberate panels instead of small indented links
- expanded block gets stronger background, border, and elevation treatment
- second-level items remain clearly subordinate but still substantial enough for an operational UI
- `Обзор` and `Настройки` align visually with the same rhythm, but do not expose disclosure behavior

## Application State Model

Replace the current single flat section state with explicit primary and secondary navigation state.

Recommended state shape:

- `activePrimary`: `overview | vk | telegram | max | whatsapp | settings`
- `activeSecondary`: `messages | groups | collection | dialogs | session | sources | webhook | null`
- `expandedPlatform`: `vk | telegram | max | whatsapp | null`

Behavior rules:

- `activeSecondary` is `null` for `overview` and `settings`
- `expandedPlatform` is purely UI state and must not itself trigger page rendering
- page rendering is determined by `activePrimary` plus `activeSecondary`
- last selected subsection per platform may be stored locally to preserve operator context when switching between platforms

## Integration With Current Frontend

The change should be introduced incrementally, not as a full rewrite.

### App Shell

`frontend/src/features/shell/AppShell.tsx` should be refactored from a flat button list into hierarchical navigation:

- render global entries for `Обзор` and `Настройки`
- render expandable platform panels for `Вконтакте`, `Телеграм`, `Max`, and `Whatsapp`
- accept primary/secondary/expanded state instead of the current flat `activeItem`
- expose callbacks for expanding a platform and selecting a subsection

### Root App Composition

`frontend/src/app/App.tsx` should become the owner of navigation state:

- hold `activePrimary`
- hold `activeSecondary`
- hold `expandedPlatform`
- map the selected navigation target to the page rendered on the right

### Page Reuse Strategy

Do not rebuild all screens at once. Reuse the current pages where practical:

- current `DashboardPage` becomes the base screen for platform `Сообщения`
- the page should support a preset platform filter so the same screen can serve `VK`, `Telegram`, `Whatsapp`, and later `Max`
- current `IntegrationsPage` should no longer represent a global integrations area
- instead, its Telegram and VK parts should be split or wrapped into narrower platform-specific screens

Initial mapping target:

- `Телеграм / Сессия` uses the current Telegram session management UI
- `Вконтакте / Группы` and `Вконтакте / Сбор` use the current VK management area, split by intent if needed
- unfinished destinations render consistent placeholder screens inside the new shell rather than falling back to the old navigation model

## Placeholder Strategy

Some destinations do not yet have full backend or frontend coverage. For those pages:

- keep the new navigation active
- render a dedicated placeholder page inside the content area
- show platform and subsection context clearly
- avoid generic text that refers to the old flat navigation structure

This keeps the information architecture stable while implementation proceeds in slices.

## Mobile Behavior

On smaller screens, the sidebar should move into an off-canvas drawer.

Rules:

- the drawer opens from a single navigation trigger in the app chrome
- accordion logic remains the same as desktop
- only one platform block is expanded at a time
- selecting a subsection closes the drawer and updates content

## Testing

Add or update frontend tests for:

- rendering top-level global entries and platform blocks
- expand/collapse behavior for platform panels
- ensuring expand does not auto-navigate
- subsection selection updates the rendered page
- preserving active visual state for a selected platform subsection
- rendering placeholders for unfinished destinations
- mobile drawer open/close behavior if implemented in the same change

## Out Of Scope

This design does not require:

- implementing every platform page in full
- adding backend support for `Max`
- redesigning auth flows
- introducing a routing library if local state remains sufficient for the current frontend scope
