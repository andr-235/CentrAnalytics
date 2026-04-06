# VK Discovery And Ingestion Design

## Goal

Add a manual VK ingestion workflow that can:

1. Search groups by region
2. Search users by region
3. Collect group posts and then collect comments for those posts
4. Collect users who publish posts or comments together with the fullest available profile data

## Recommended Approach

Use a staged hybrid architecture:

- Official VK API is the primary integration path
- A non-official fallback collector is used only when official VK APIs cannot provide the required data or search coverage
- Discovery data and crawl jobs are stored in VK-specific tables
- Normalized entities continue to flow into the existing integration domain through `IntegrationSource`, `Conversation`, `Message`, `ExternalUser`, and `RawEvent`

This keeps the current webhook-based VK and Telegram ingestion intact while adding a pull-based discovery and crawl pipeline for VK.

## Runtime Design

The VK integration is split into four manually triggered scenarios exposed through admin API endpoints:

1. `group discovery`
2. `user discovery`
3. `group post crawl`
4. `engagement crawl`

Each request creates a `VkCrawlJob` that tracks the lifecycle of the operation. The orchestration flow is:

1. Create a job in `CREATED`
2. Move it to `RUNNING`
3. Execute the official VK client
4. Invoke fallback collection only for explicitly defined gaps
5. Persist raw and VK-specific snapshots
6. Normalize discovered entities into the current integration model
7. Mark the job as `COMPLETED`, `PARTIAL`, or `FAILED`

## Data Model

The design adds a VK-specific discovery layer rather than overloading the existing normalized tables with source-specific concerns.

### New entities

- `VkCrawlJob`
  Stores manual job type, status, request parameters JSON, initiation metadata, item counters, warnings, error details, and timestamps.

- `VkGroupCandidate`
  Stores group search results with `vk_group_id`, `screen_name`, name, region matching result, match source, source method, and raw JSON.

- `VkUserCandidate`
  Stores user search or author discovery results with `vk_user_id`, rich profile fields, region matching result, discovery source, source method, and raw JSON.

- `VkWallPostSnapshot`
  Stores raw and normalized fields for a VK wall post identified by `owner_id + post_id`.

- `VkCommentSnapshot`
  Stores raw and normalized fields for a VK comment identified by `owner_id + post_id + comment_id`.

### Normalized integration mapping

- VK group -> `IntegrationSource`
- VK post thread or group wall context -> `Conversation`
- VK post/comment -> `Message`
- VK author -> `ExternalUser`

VK-specific entities remain the system of record for crawl provenance and source fidelity, while the normalized integration model remains the queryable analytical layer.

## Region Matching Rules

Region filtering must use two layers:

1. Structured VK fields first
   - City
   - Region references exposed by official APIs
   - Group location metadata when available

2. Text-based matching second
   - Group description
   - User status and profile text
   - Post text
   - Other searchable source text returned by VK or the fallback collector

The system should record how the region match was established through a field such as `match_source` so downstream users can distinguish between strong and weak matches.

## API Design

Manual admin endpoints:

- `POST /api/admin/integrations/vk/groups/search`
- `POST /api/admin/integrations/vk/users/search`
- `POST /api/admin/integrations/vk/groups/{groupId}/posts/collect`
- `POST /api/admin/integrations/vk/posts/comments/collect`
- `POST /api/admin/integrations/vk/users/enrich`
- `GET /api/admin/integrations/vk/jobs/{jobId}`

The request payloads should include region criteria, limits, collection mode, and other task-specific filters. The response for `POST` endpoints should return job metadata immediately rather than keeping the HTTP request open until the crawl finishes.

## Fallback Policy

The non-official collector must be tightly controlled. It should be used only when:

- Structured region data is absent and text-based region discovery is required
- Official API access cannot return the required profile breadth
- Post or comment author data is incomplete
- Official API restrictions or limits block the requested collection path

Fallback execution must be traceable. Every persisted candidate, snapshot, or normalized record should include the collection method or source channel so operators can distinguish between official and fallback data.

## Error Handling

- Network and rate-limit failures should be accumulated at job level and reflected in job statistics
- Individual item failures should not terminate the entire job unless the whole request becomes unrecoverable
- Jobs must support `COMPLETED`, `PARTIAL`, and `FAILED` outcomes
- Unique constraints and upsert logic must deduplicate:
  - group: `vk_group_id`
  - user: `vk_user_id`
  - post: `owner_id + post_id`
  - comment: `owner_id + post_id + comment_id`

## Testing Strategy

Add the following test layers:

- Unit tests for region matching, fallback policy, request validation, and VK mappers
- Persistence tests for new entities, unique keys, and normalized linking
- Spring MVC tests for the new admin endpoints and job lifecycle responses
- Integration tests for the orchestration service using mocked official and fallback client responses

Real VK network calls are out of scope for automated tests in the first version.

## Why This Design

- Fits the existing `integration` ingestion model instead of bypassing it
- Keeps VK-specific search and crawl concerns isolated from normalized analytics entities
- Supports manual operation first, which matches the approved workflow
- Allows the system to start with official APIs and add fallback collection only where needed
- Preserves traceability for data origin, which is critical once hybrid collection is introduced
