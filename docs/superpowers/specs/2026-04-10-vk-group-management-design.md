# VK Group Management Design

## Goal

Add manual VK group management so operators can:

1. List already discovered groups
2. Delete one or several groups together with all group-linked discovery data
3. Launch collection only for one or several groups that already exist in `vk_group_candidate`

## Scope

This design is intentionally limited to groups that are already present in the local database. Manual operator input may use different identifiers, but resolution must happen only against existing `vk_group_candidate` rows. The system must not create missing groups during manual collect.

## Recommended Approach

Add a small VK group management layer above the existing discovery and crawl services:

- keep `VkDiscoveryOrchestrator` and `VkCrawlCommandService` as the execution path
- add a dedicated resolver/service that can map arbitrary operator input to existing `VkGroupCandidate`
- expose separate admin endpoints for collect and delete batch operations
- keep auto-collection behavior unchanged except where manual collect reuses the same post/comment jobs

This keeps the new feature operationally useful without reopening the region-search pipeline or introducing a second storage model for groups.

## Input Resolution Rules

Manual group operations must accept a list of arbitrary string identifiers. Each identifier is resolved only against existing local candidates using this order:

1. `vk_group_candidate.id`
2. `vk_group_candidate.vk_group_id`
3. `vk_group_candidate.screen_name`
4. VK aliases like `club123` and `public123`
5. VK URLs such as `https://vk.com/club123`, `https://vk.com/public123`, or `https://vk.com/screen_name`

If an identifier is syntactically valid but does not match an existing candidate, it must be reported as not found. The operation should continue for other valid matches.

## API Design

Add admin endpoints under the VK manual-management surface:

- `GET /api/admin/integrations/vk/groups`
  Returns the existing `vk_group_candidate` list with optional search filtering, reusing the current read model where possible.

- `POST /api/admin/integrations/vk/groups/collect`
  Request body: list of string identifiers.
  Response: resolved groups, unresolved identifiers, and created crawl jobs.

- `DELETE /api/admin/integrations/vk/groups`
  Request body: list of string identifiers.
  Response: resolved groups, unresolved identifiers, and removed group summary.

For collect and delete, partial success is acceptable. The response must distinguish between:

- resolved and processed groups
- duplicate identifiers collapsed to one group
- identifiers that could not be resolved in the local DB

## Collect Behavior

Manual collect by groups must not create `GROUP_SEARCH` or `USER_SEARCH`.

For every resolved group:

1. create `GROUP_POSTS`
2. read the freshest posts for that group from `vk_wall_post_snapshot`
3. create `POST_COMMENTS` for the configured post subset if posts exist

This reuses the existing crawl path and preserves current ingestion into `IntegrationSource` and downstream entities.

## Delete Behavior

Deleting a group means deleting the group and all group-linked discovery data:

- `vk_comment_snapshot` for the group owner
- `vk_wall_post_snapshot` for the group owner
- `vk_group_candidate`
- the linked `integration_source` for that VK group

`vk_user_candidate` must remain untouched because users are collected from activity and may belong to multiple groups. Deleting one group must not delete shared user entities.

Delete order matters. The service should remove dependent comment/post snapshots first, then the group candidate, then the group-linked `integration_source`.

## Service Design

Add a focused management service, for example `VkGroupManagementService`, responsible for:

- parsing and normalizing operator input
- resolving identifiers to existing `VkGroupCandidate`
- launching manual post/comment jobs for selected groups
- deleting selected groups and all group-linked records

Add repository helpers where needed for:

- lookup by `screen_name`
- bulk lookup by `vk_group_id`
- delete snapshots by `owner_id`
- find/delete `IntegrationSource` by VK group external id

## Error Handling

- Invalid or unresolved identifiers must not fail the whole request if at least one group resolves.
- Duplicate identifiers that resolve to the same group must be deduplicated before execution.
- Delete operations must be transactional per request so partial row cleanup does not leave a group half-deleted.
- Collect operations should report created jobs even when some identifiers are unresolved.

## Testing Strategy

Add:

- resolver unit tests for numeric ids, `vkGroupId`, `screen_name`, `club/public`, and URL inputs
- service tests for collect-by-groups and delete-by-groups behavior
- MVC tests for the new admin endpoints and partial-success payloads
- persistence tests for cascade-like cleanup of posts, comments, candidate, and `integration_source`

## Why This Design

- matches the operator workflow exactly: work only with groups already known to the system
- keeps manual collect narrow and predictable
- avoids reintroducing VK user-search rate-limit problems
- isolates group-management logic from the existing discovery orchestrator
