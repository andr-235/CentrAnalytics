# VK Official Only Design

## Goal

Remove all non-official VK parsing and fallback behavior so the VK integration works only through the official VK API/SDK.

## Scope

This design is limited to the backend VK integration. It keeps the existing VK endpoints and crawl jobs where they still make sense under the official API, but removes every HTML/JS scraping path, fallback policy, and hybrid mode.

## Recommended Approach

Simplify the VK integration to a single execution path:

- keep `VkOfficialClient` as the only VK client
- delete fallback parsing clients and helper parsers
- remove `HYBRID` and `FALLBACK` behavior from orchestration, config, and tests
- preserve existing admin/job entry points where they can already be served by the official API

This keeps the external integration surface stable enough for the UI while eliminating the part of the implementation that triggered token and platform policy risk.

## Functional Design

The following workflows remain supported only through the official VK API:

1. group search
2. user search
3. group post collection
4. post comment collection
5. user enrichment

If a workflow already uses an official method, it should continue to work through that method only. If a code path exists only to support fallback parsing, that path must be removed rather than silently replaced by another unofficial mechanism.

## Configuration Changes

Keep only official VK settings:

- `integration.vk.group-id`
- `integration.vk.access-token`
- `integration.vk.api-version`
- `integration.vk.api-base-url`
- `integration.vk.request-timeout`

Remove fallback-only or hybrid-only settings:

- `integration.vk.user-access-token`
- `integration.vk.fallback-base-url`
- `integration.vk.auto-collection.collection-mode`

The auto-collection scheduler and request DTOs should no longer depend on collection mode switching.

## Code Changes

Delete the non-official VK layer:

- `HttpVkFallbackClient`
- `VkFallbackClient`
- `NoopVkFallbackClient`
- `VkFallbackPolicy`
- JS/JSON parsing helpers used only by fallback scraping

Refactor orchestration and services so they call only `VkOfficialClient`. Any `useFallback`, `shouldFallback`, `HYBRID`, or `FALLBACK` branch must be removed.

`VkCollectionMethod` should be reduced to the official path unless another still-valid internal value is needed for historical persisted rows. New writes must always use `OFFICIAL_API`.

## API and UI Compatibility

Keep the current backend endpoints and request/response contracts unless a field exists only to expose fallback/hybrid behavior. In that case, remove the field and update the frontend/tests accordingly.

The intended outcome is operational simplification, not a broader VK feature redesign.

## Error Handling

When the official API returns no data, lacks permissions, or rejects a call, the job should complete or fail according to the current job model without retrying through parsing.

The system must be explicit:

- no hidden fallback
- no unofficial retry path
- no parsing of VK web pages or embedded payloads

## Testing Strategy

Update tests to validate official-only behavior:

- config binding tests for reduced VK properties
- service/orchestrator tests without fallback branches
- client tests for `HttpVkOfficialClient`
- deletion of fallback-specific tests and fixtures

Regression coverage should confirm that search and collection jobs still run through the official client and that no fallback classes remain wired in the Spring context.

## Risks and Constraints

- Some scenarios may now return fewer results than the old hybrid implementation.
- Official API permissions and token scopes become the only supported capability boundary.
- Existing database rows marked as `FALLBACK` may remain historically, but the application must stop creating new ones.

## Why This Design

- removes the policy and maintenance risk from unofficial VK scraping
- makes VK behavior predictable and supportable
- reduces config, branching, and test surface
- aligns implementation with the operational requirement to use only official VK APIs
