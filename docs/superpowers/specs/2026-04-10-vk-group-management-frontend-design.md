# VK Group Management Frontend Design

## Goal

Update the integrations UI so operators can manage existing VK groups directly from the current VK panel:

- see the full group list instead of only the first few rows
- scroll through the list in-place
- select one or many groups
- run collect for one or many selected groups
- delete one or many selected groups

## UX Direction

Keep everything inside the existing VK section on the integrations page. Do not add a separate screen.

The VK panel should provide:

- a scrollable list container with a fixed header row
- checkboxes for multi-select
- batch action buttons in the panel header or action bar
- single-row quick actions for collect and delete
- inline feedback for successful actions, partial success, and unresolved identifiers

This preserves the current operator workflow and avoids unnecessary navigation.

## Data Flow

Frontend already loads `GET /api/admin/integrations/vk/groups`. Extend it with:

- `POST /api/admin/integrations/vk/groups/collect`
- `DELETE /api/admin/integrations/vk/groups`

The page should send string identifiers. Since every row already knows `id`, the frontend can use that as the canonical identifier for selected rows. Row quick actions can also use `id`.

After any successful mutation:

- show a compact status message
- refresh the integrations snapshot

## State Model

Add local state for:

- selected VK group ids
- mutation pending flag
- VK action result message

Selection should support:

- individual row toggles
- select-all on visible list
- automatic cleanup when refreshed data removes a previously selected group

## Rendering Rules

- Remove the hard `slice(0, 6)` cap
- Show the full list inside a vertically scrollable container
- Keep existing group metadata visible
- Add a dedicated actions column

If no groups exist, keep the current empty-state message.

## Error Handling

- If a batch action returns unresolved identifiers, surface them in the VK panel
- If the request fails, keep current selection and show an error message
- Disable action buttons while a request is running

## Testing

Add frontend tests for:

- rendering the full list
- single-row collect/delete actions
- batch collect/delete actions with selected rows
- refresh after successful mutation
- inline feedback rendering
