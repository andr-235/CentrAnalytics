# Overview Analytics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `Обзор` placeholder with a real analytics cockpit backed by a dedicated `/api/overview` endpoint and a new frontend overview page.

**Architecture:** Add a dedicated backend overview query slice that aggregates platform traffic, trend buckets, and integration health without reusing the raw message list endpoint. On the frontend, render a new `OverviewPage` that owns the selected time window and displays global KPIs plus per-platform sections, while keeping the existing `DashboardPage` as the platform-specific messages screen.

**Tech Stack:** Spring Boot, Spring MVC, JPA Criteria/JPQL aggregation, JUnit 5, MockMvc, React, TypeScript, Testing Library, existing global CSS

---

## File Structure

### Backend

- Create: `src/main/java/com/ca/centranalytics/integration/api/controller/OverviewController.java`
- Create: `src/main/java/com/ca/centranalytics/integration/api/service/OverviewQueryService.java`
- Create: `src/main/java/com/ca/centranalytics/integration/api/dto/OverviewResponse.java`
- Create: `src/main/java/com/ca/centranalytics/integration/api/dto/PlatformOverviewResponse.java`
- Create: `src/main/java/com/ca/centranalytics/integration/api/dto/OverviewSummaryResponse.java`
- Create: `src/main/java/com/ca/centranalytics/integration/api/dto/OverviewHighlightResponse.java`
- Create: `src/main/java/com/ca/centranalytics/integration/api/dto/OverviewTrendPointResponse.java`
- Create: `src/main/java/com/ca/centranalytics/integration/api/dto/PlatformIntegrationStatusResponse.java`
- Create: `src/main/java/com/ca/centranalytics/integration/api/dto/PlatformAttentionItemResponse.java`
- Create: `src/main/java/com/ca/centranalytics/integration/api/dto/OverviewWindow.java`
- Create: `src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewMetricsRepository.java`
- Create: `src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewMetricsRepositoryImpl.java`
- Create: `src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewPlatformMetrics.java`
- Create: `src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewSummaryMetrics.java`
- Create: `src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewTrendBucket.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/domain/repository/IntegrationSourceRepository.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/telegram/user/domain/TelegramUserSessionRepository.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/repository/VkGroupCandidateRepository.java`
- Test: `src/test/java/com/ca/centranalytics/integration/api/service/OverviewQueryServiceTest.java`
- Test: `src/test/java/com/ca/centranalytics/integration/api/controller/OverviewControllerIntegrationTest.java`

### Frontend

- Create: `frontend/src/features/overview/OverviewPage.tsx`
- Create: `frontend/src/features/overview/overview.api.ts`
- Create: `frontend/src/features/overview/overview.types.ts`
- Create: `frontend/src/features/overview/OverviewPage.test.tsx`
- Modify: `frontend/src/app/App.tsx`
- Modify: `frontend/src/app/App.test.tsx`
- Modify: `frontend/src/shared/styles/global.css`

### Notes

- Keep `frontend/src/features/dashboard/DashboardPage.tsx` unchanged except for any import fallout; it remains the platform `Сообщения` screen.
- Do not add Flyway migrations in this slice unless implementation uncovers a hard data gap. The spec allows `unknown` or `inactive` states when reliable signals do not exist.

### Task 1: Add failing backend service tests for overview aggregation

**Files:**
- Create: `src/test/java/com/ca/centranalytics/integration/api/service/OverviewQueryServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
@ExtendWith(MockitoExtension.class)
class OverviewQueryServiceTest {

    @Mock
    private OverviewMetricsRepository overviewMetricsRepository;

    @Mock
    private IntegrationSourceRepository integrationSourceRepository;

    @Mock
    private TelegramUserSessionRepository telegramUserSessionRepository;

    @Mock
    private VkGroupCandidateRepository vkGroupCandidateRepository;

    @InjectMocks
    private OverviewQueryService overviewQueryService;

    @Test
    void getOverview_buildsSummaryAndPlatformStatuses() {
        Instant now = Instant.parse("2026-04-10T10:15:00Z");
        when(overviewMetricsRepository.fetchSummary(any(), any()))
                .thenReturn(new OverviewSummaryMetrics(120, 18, 44));
        when(overviewMetricsRepository.fetchPlatformMetrics(any(), any()))
                .thenReturn(List.of(
                        new OverviewPlatformMetrics(Platform.TELEGRAM, 90, 12, 30),
                        new OverviewPlatformMetrics(Platform.VK, 30, 6, 14)
                ));

        OverviewResponse response = overviewQueryService.getOverview(OverviewWindow.HOURS_24, now);

        assertThat(response.summary().messageCount()).isEqualTo(120);
        assertThat(response.platforms()).extracting(PlatformOverviewResponse::platform)
                .containsExactly(Platform.TELEGRAM, Platform.VK, Platform.WHATSAPP, Platform.MAX);
        assertThat(response.platforms()).filteredOn(item -> item.platform() == Platform.TELEGRAM)
                .first()
                .extracting(PlatformOverviewResponse::status)
                .isEqualTo("healthy");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=OverviewQueryServiceTest test`
Expected: FAIL because `OverviewQueryService` and overview DTO/repository types do not exist yet.

- [ ] **Step 3: Write minimal DTO and service skeletons**

```java
public enum OverviewWindow {
    HOURS_24(Duration.ofHours(24)),
    DAYS_7(Duration.ofDays(7)),
    DAYS_30(Duration.ofDays(30));
}

@Service
@RequiredArgsConstructor
public class OverviewQueryService {

    private final OverviewMetricsRepository overviewMetricsRepository;
    private final IntegrationSourceRepository integrationSourceRepository;
    private final TelegramUserSessionRepository telegramUserSessionRepository;
    private final VkGroupCandidateRepository vkGroupCandidateRepository;

    public OverviewResponse getOverview(OverviewWindow window, Instant now) {
        throw new UnsupportedOperationException("Implement in next task");
    }
}
```

- [ ] **Step 4: Run test to verify it now fails on missing behavior**

Run: `./mvnw -Dtest=OverviewQueryServiceTest test`
Expected: FAIL with assertion or unsupported operation, not compiler errors.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/ca/centranalytics/integration/api/service/OverviewQueryServiceTest.java src/main/java/com/ca/centranalytics/integration/api/service/OverviewQueryService.java src/main/java/com/ca/centranalytics/integration/api/dto src/main/java/com/ca/centranalytics/integration/domain/repository/Overview*
git commit -m "test: scaffold overview aggregation service"
```

### Task 2: Implement backend aggregation query layer

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewMetricsRepository.java`
- Create: `src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewMetricsRepositoryImpl.java`
- Create: `src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewPlatformMetrics.java`
- Create: `src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewSummaryMetrics.java`
- Create: `src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewTrendBucket.java`

- [ ] **Step 1: Implement summary and per-platform aggregate queries**

```java
public interface OverviewMetricsRepository {
    OverviewSummaryMetrics fetchSummary(Instant from, Instant to);
    List<OverviewPlatformMetrics> fetchPlatformMetrics(Instant from, Instant to);
    List<OverviewTrendBucket> fetchTrend(Platform platform, Instant from, Instant to, OverviewWindow window);
}
```

```java
@Repository
public class OverviewMetricsRepositoryImpl implements OverviewMetricsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public OverviewSummaryMetrics fetchSummary(Instant from, Instant to) {
        // count messages, distinct conversations, distinct authors for the requested window
    }
}
```

- [ ] **Step 2: Bucket trend data at repository level**

```java
private Instant truncateToBucket(Instant instant, OverviewWindow window) {
    return switch (window) {
        case HOURS_24 -> instant.truncatedTo(ChronoUnit.HOURS);
        case DAYS_7, DAYS_30 -> instant.truncatedTo(ChronoUnit.DAYS);
    };
}
```

- [ ] **Step 3: Add focused repository unit coverage through service test fixtures**

```java
when(overviewMetricsRepository.fetchTrend(Platform.TELEGRAM, from, to, OverviewWindow.HOURS_24))
        .thenReturn(List.of(new OverviewTrendBucket(Instant.parse("2026-04-10T09:00:00Z"), 12)));
```

- [ ] **Step 4: Run the backend test slice**

Run: `./mvnw -Dtest=OverviewQueryServiceTest test`
Expected: FAIL only on service assembly/status logic that is still missing.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewMetricsRepository.java src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewMetricsRepositoryImpl.java src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewPlatformMetrics.java src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewSummaryMetrics.java src/main/java/com/ca/centranalytics/integration/domain/repository/OverviewTrendBucket.java
git commit -m "feat: add overview metrics query layer"
```

### Task 3: Implement overview service assembly and platform health rules

**Files:**
- Modify: `src/main/java/com/ca/centranalytics/integration/api/service/OverviewQueryService.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/domain/repository/IntegrationSourceRepository.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/telegram/user/domain/TelegramUserSessionRepository.java`
- Modify: `src/main/java/com/ca/centranalytics/integration/channel/vk/repository/VkGroupCandidateRepository.java`

- [ ] **Step 1: Add repository helpers for health sources**

```java
public interface IntegrationSourceRepository extends JpaRepository<IntegrationSource, Long> {
    List<IntegrationSource> findByPlatform(Platform platform);
    long countByPlatform(Platform platform);
}

public interface VkGroupCandidateRepository extends JpaRepository<VkGroupCandidate, Long> {
    long count();
}
```

- [ ] **Step 2: Assemble summary, platform sections, and attention items**

```java
public OverviewResponse getOverview(OverviewWindow window, Instant now) {
    Instant from = now.minus(window.duration());
    Instant to = now;

    OverviewSummaryMetrics summary = overviewMetricsRepository.fetchSummary(from, to);

    List<PlatformOverviewResponse> platforms = Stream.of(Platform.TELEGRAM, Platform.VK, Platform.WHATSAPP, Platform.MAX)
            .map(platform -> buildPlatformOverview(platform, from, to, now, window, summary.messageCount()))
            .toList();

    long issueCount = platforms.stream()
            .filter(item -> !"healthy".equals(item.status()))
            .count();

    return new OverviewResponse(
            now,
            window.apiValue(),
            new OverviewSummaryResponse(summary.messageCount(), summary.conversationCount(), summary.activeAuthorCount(), issueCount),
            platforms
    );
}
```

- [ ] **Step 3: Encode pragmatic first-pass status rules**

```java
private String resolveStatus(Platform platform, PlatformContext context, Instant now) {
    if (context.sourceCount() == 0 && context.messageCount() == 0) {
        return "inactive";
    }
    if (platform == Platform.TELEGRAM && context.telegramSessionState() == TelegramUserSessionState.WAIT_PASSWORD) {
        return "warning";
    }
    if (platform == Platform.TELEGRAM && context.telegramSessionState() == TelegramUserSessionState.FAILED) {
        return "critical";
    }
    if (context.lastEventAt() != null && context.lastEventAt().isBefore(now.minus(Duration.ofHours(6)))) {
        return "warning";
    }
    return "healthy";
}
```

- [ ] **Step 4: Run service tests**

Run: `./mvnw -Dtest=OverviewQueryServiceTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ca/centranalytics/integration/api/service/OverviewQueryService.java src/main/java/com/ca/centranalytics/integration/domain/repository/IntegrationSourceRepository.java src/main/java/com/ca/centranalytics/integration/channel/telegram/user/domain/TelegramUserSessionRepository.java src/main/java/com/ca/centranalytics/integration/channel/vk/repository/VkGroupCandidateRepository.java
git commit -m "feat: assemble overview platform analytics"
```

### Task 4: Add failing backend API integration test and controller

**Files:**
- Create: `src/main/java/com/ca/centranalytics/integration/api/controller/OverviewController.java`
- Create: `src/test/java/com/ca/centranalytics/integration/api/controller/OverviewControllerIntegrationTest.java`

- [ ] **Step 1: Write the failing integration test**

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OverviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getOverview_returnsPlatformSectionsForSelectedWindow() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/overview")
                        .param("window", "24h")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.window").value("24h"))
                .andExpect(jsonPath("$.summary.messageCount").exists())
                .andExpect(jsonPath("$.platforms.length()").value(4));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=OverviewControllerIntegrationTest test`
Expected: FAIL with 404 or missing controller.

- [ ] **Step 3: Add the controller and request binding**

```java
@RestController
@RequestMapping("/api/overview")
@RequiredArgsConstructor
public class OverviewController {

    private final OverviewQueryService overviewQueryService;

    @GetMapping
    public OverviewResponse getOverview(@RequestParam(defaultValue = "24h") OverviewWindow window) {
        return overviewQueryService.getOverview(window, Instant.now());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=OverviewControllerIntegrationTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ca/centranalytics/integration/api/controller/OverviewController.java src/test/java/com/ca/centranalytics/integration/api/controller/OverviewControllerIntegrationTest.java
git commit -m "feat: expose overview analytics endpoint"
```

### Task 5: Add failing frontend tests for overview page and app wiring

**Files:**
- Create: `frontend/src/features/overview/OverviewPage.test.tsx`
- Modify: `frontend/src/app/App.test.tsx`

- [ ] **Step 1: Write a failing overview page test**

```tsx
it("renders summary cards and platform sections for the selected window", async () => {
  const loadOverview = vi.fn().mockResolvedValue({
    ok: true as const,
    data: {
      generatedAt: "2026-04-10T10:15:00Z",
      window: "24h",
      summary: { messageCount: 120, conversationCount: 18, activeAuthorCount: 44, platformIssueCount: 1 },
      platforms: [
        { platform: "TELEGRAM", label: "Telegram", status: "healthy", highlights: [], trend: [], integration: { syncStatus: "healthy", sourceCount: 1, lastEventAt: "2026-04-10T10:00:00Z", lastSuccessAt: "2026-04-10T10:00:00Z", lastErrorMessage: null }, attentionItems: [] }
      ]
    }
  });

  render(<OverviewPage token="token" loadOverview={loadOverview} />);

  expect(await screen.findByText("120")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: /7д/i })).toBeInTheDocument();
  expect(screen.getByText(/telegram/i)).toBeInTheDocument();
});
```

- [ ] **Step 2: Write a failing app test for overview as the default authenticated page**

```tsx
it("renders the overview analytics page when there is a saved token", async () => {
  window.localStorage.setItem("centranalytics.token", "demo-token");
  vi.spyOn(window, "fetch").mockResolvedValue(new Response(JSON.stringify(mockOverviewPayload), {
    status: 200,
    headers: { "Content-Type": "application/json" }
  }));

  render(<App />);

  expect(await screen.findByRole("heading", { name: /обзор/i })).toBeInTheDocument();
  expect(screen.getByText(/telegram/i)).toBeInTheDocument();
});
```

- [ ] **Step 3: Run the focused frontend tests**

Run: `npm --prefix frontend test -- --runInBand src/features/overview/OverviewPage.test.tsx src/app/App.test.tsx`
Expected: FAIL because `OverviewPage` and overview fetch wiring do not exist yet.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/features/overview/OverviewPage.test.tsx frontend/src/app/App.test.tsx
git commit -m "test: add overview page coverage"
```

### Task 6: Implement overview frontend data layer and page

**Files:**
- Create: `frontend/src/features/overview/overview.types.ts`
- Create: `frontend/src/features/overview/overview.api.ts`
- Create: `frontend/src/features/overview/OverviewPage.tsx`

- [ ] **Step 1: Add typed API models and fetch helper**

```ts
export type OverviewWindow = "24h" | "7d" | "30d";

export type OverviewResult =
  | { ok: true; data: OverviewSnapshot }
  | { ok: false; unauthorized: boolean; error: string };

export async function fetchOverview(token: string, window: OverviewWindow): Promise<OverviewResult> {
  const response = await fetch(`${resolveApiBaseUrl()}/api/overview?window=${window}`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  // map 200 / 401 / generic failure to the union above
}
```

- [ ] **Step 2: Implement the page with window switching and refresh**

```tsx
export function OverviewPage({ token, loadOverview = fetchOverview, onUnauthorized }: OverviewPageProps) {
  const [window, setWindow] = useState<OverviewWindow>("24h");
  const [snapshot, setSnapshot] = useState<OverviewSnapshot | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  async function refresh(nextWindow = window) {
    const result = await loadOverview(token, nextWindow);
    if (!result.ok) {
      if (result.unauthorized) {
        onUnauthorized?.();
        return;
      }
      setError(result.error);
      return;
    }
    setSnapshot(result.data);
  }
}
```

- [ ] **Step 3: Render stable first-pass UI primitives**

```tsx
<section className="overview-summary-grid">
  <article><span>Сообщения</span><strong>{snapshot.summary.messageCount}</strong></article>
  <article><span>Диалоги</span><strong>{snapshot.summary.conversationCount}</strong></article>
</section>

{snapshot.platforms.map((platform) => (
  <section key={platform.platform} className={`overview-platform overview-platform--${platform.status}`}>
    <h2>{platform.label}</h2>
  </section>
))}
```

- [ ] **Step 4: Run focused frontend tests**

Run: `npm --prefix frontend test -- --runInBand src/features/overview/OverviewPage.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/features/overview/overview.types.ts frontend/src/features/overview/overview.api.ts frontend/src/features/overview/OverviewPage.tsx
git commit -m "feat: add overview analytics page"
```

### Task 7: Wire overview into the app shell and add styling

**Files:**
- Modify: `frontend/src/app/App.tsx`
- Modify: `frontend/src/shared/styles/global.css`
- Modify: `frontend/src/app/App.test.tsx`

- [ ] **Step 1: Replace the placeholder overview route**

```tsx
if (activePrimary === "overview") {
  return <OverviewPage token={token} onUnauthorized={clearSession} />;
}
```

- [ ] **Step 2: Add page-specific styles for summary cards, window controls, and platform sections**

```css
.overview-shell {
  display: grid;
  gap: 24px;
}

.overview-platform {
  padding: 24px;
  border: 1px solid var(--panel-border);
  background: linear-gradient(180deg, rgba(255,255,255,0.48), rgba(255,255,255,0.14)), var(--panel-bg);
}
```

- [ ] **Step 3: Update app tests to assert the real overview content**

```tsx
expect(await screen.findByRole("heading", { name: /обзор/i })).toBeInTheDocument();
expect(screen.getByText(/platforms with issues/i)).not.toBeInTheDocument();
expect(screen.getByText(/telegram/i)).toBeInTheDocument();
```

- [ ] **Step 4: Run focused app tests**

Run: `npm --prefix frontend test -- --runInBand src/app/App.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/App.tsx frontend/src/shared/styles/global.css frontend/src/app/App.test.tsx
git commit -m "feat: wire overview analytics into app shell"
```

### Task 8: Full verification and cleanup

**Files:**
- None required unless fixes are needed

- [ ] **Step 1: Run backend verification**

Run: `./mvnw -Dtest=OverviewQueryServiceTest,OverviewControllerIntegrationTest test`
Expected: PASS with both overview backend tests green.

- [ ] **Step 2: Run frontend verification**

Run: `npm --prefix frontend test -- --runInBand src/features/overview/OverviewPage.test.tsx src/app/App.test.tsx`
Expected: PASS with overview page and app routing coverage green.

- [ ] **Step 3: Run a combined smoke pass if the environment allows**

Run: `./mvnw test`
Expected: PASS, or document unrelated failures if the suite is already red outside this feature.

- [ ] **Step 4: Review for spec coverage before handing off**

Checklist:
- global summary row implemented
- four platform sections always render
- `24h / 7d / 30d` window switching works
- partial/inactive states render safely
- `DashboardPage` remains the platform messages page

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: add overview analytics cockpit"
```
