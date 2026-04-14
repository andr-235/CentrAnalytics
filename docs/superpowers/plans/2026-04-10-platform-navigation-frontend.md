# Platform Navigation Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the flat frontend sidebar with platform-first accordion navigation, preserve working message and integration flows, and move the app to primary/secondary navigation state.

**Architecture:** Keep the current React/Vite shell and migrate in place. `App.tsx` becomes the owner of `activePrimary`, `activeSecondary`, and `expandedPlatform`; `AppShell.tsx` becomes hierarchical navigation; existing content pages are reused where possible by wrapping Telegram/VK operations into narrower screens and keeping unfinished destinations as explicit placeholders.

**Tech Stack:** React 19, TypeScript, Vite, Testing Library, Vitest, CSS in `frontend/src/shared/styles/global.css`

---

## File Structure

### Existing files to modify

- `frontend/src/app/App.tsx`
  Owns authenticated shell state and chooses the page rendered on the right.
- `frontend/src/app/App.test.tsx`
  Verifies auth fallback, authenticated rendering, and unauthorized reset behavior.
- `frontend/src/features/shell/AppShell.tsx`
  Renders the left navigation shell.
- `frontend/src/features/shell/AppShell.test.tsx`
  Verifies navigation rendering and interactions.
- `frontend/src/features/dashboard/DashboardPage.tsx`
  Current message table page that can be reused for platform message screens.
- `frontend/src/shared/styles/global.css`
  Holds shell/navigation styling and responsive behavior.

### New files to create

- `frontend/src/features/shell/navigation.types.ts`
  Shared navigation types and menu definitions for `App` and `AppShell`.
- `frontend/src/features/shell/PlaceholderPage.tsx`
  Reusable placeholder content for unfinished destinations.
- `frontend/src/features/integrations/TelegramSessionPage.tsx`
  Telegram-specific wrapper or extracted panel for `Телеграм / Сессия`.
- `frontend/src/features/integrations/VkGroupsPage.tsx`
  VK-specific wrapper or extracted panel for `Вконтакте / Группы` and `Вконтакте / Сбор`.

### Existing files to extend if extraction is needed

- `frontend/src/features/integrations/IntegrationsPage.tsx`
  Source for Telegram/VK operational blocks during extraction.
- `frontend/src/features/integrations/IntegrationsPage.test.tsx`
  Reference test coverage for Telegram session and VK group operations.

---

### Task 1: Define Navigation Model And Lock UI Behavior In Tests

**Files:**
- Create: `frontend/src/features/shell/navigation.types.ts`
- Modify: `frontend/src/features/shell/AppShell.test.tsx`
- Modify: `frontend/src/app/App.test.tsx`

- [ ] **Step 1: Write failing shell tests for platform-first navigation**

```tsx
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { AppShell } from "./AppShell";

describe("AppShell", () => {
  it("renders overview, settings, and platform accordion blocks", () => {
    render(
      <AppShell
        activePrimary="overview"
        activeSecondary={null}
        expandedPlatform={null}
      >
        <div>Content</div>
      </AppShell>
    );

    expect(screen.getByRole("button", { name: /обзор/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /вконтакте/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /телеграм/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /max/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /whatsapp/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /настройки/i })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /^группы$/i })).not.toBeInTheDocument();
  });

  it("expands a platform without navigating to content", async () => {
    const user = userEvent.setup();
    const onTogglePlatform = vi.fn();
    const onSelectItem = vi.fn();

    render(
      <AppShell
        activePrimary="overview"
        activeSecondary={null}
        expandedPlatform={null}
        onTogglePlatform={onTogglePlatform}
        onSelectItem={onSelectItem}
      >
        <div>Content</div>
      </AppShell>
    );

    await user.click(screen.getByRole("button", { name: /телеграм/i }));

    expect(onTogglePlatform).toHaveBeenCalledWith("telegram");
    expect(onSelectItem).not.toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: Write failing app-level tests for primary/secondary rendering**

```tsx
import { render, screen } from "@testing-library/react";

import App from "./App";

describe("App", () => {
  it("renders overview entry points when there is a saved token", async () => {
    window.localStorage.setItem("centranalytics.token", "demo-token");
    vi.spyOn(window, "fetch").mockResolvedValue(
      new Response(JSON.stringify([]), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      })
    );

    render(<App />);

    expect(await screen.findByRole("button", { name: /обзор/i })).toBeInTheDocument();
    expect(screen.getByText(/выберите раздел платформы/i)).toBeInTheDocument();
  });
});
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```bash
npm --prefix frontend test -- AppShell.test.tsx App.test.tsx --runInBand
```

Expected:

```text
FAIL frontend/src/features/shell/AppShell.test.tsx
FAIL frontend/src/app/App.test.tsx
```

- [ ] **Step 4: Add shared navigation types and menu definitions**

```ts
export type PrimarySection =
  | "overview"
  | "vk"
  | "telegram"
  | "max"
  | "whatsapp"
  | "settings";

export type PlatformSection = Extract<
  PrimarySection,
  "vk" | "telegram" | "max" | "whatsapp"
>;

export type SecondarySection =
  | "messages"
  | "groups"
  | "collection"
  | "dialogs"
  | "session"
  | "sources"
  | "webhook";

export type NavigationSelection = {
  primary: PrimarySection;
  secondary: SecondarySection | null;
};

export const platformMenu = {
  vk: ["messages", "groups", "collection"],
  telegram: ["messages", "dialogs", "session"],
  max: ["messages", "sources"],
  whatsapp: ["messages", "webhook", "sources"]
} as const satisfies Record<PlatformSection, readonly SecondarySection[]>;
```

- [ ] **Step 5: Refactor tests to use the new navigation props and expectations**

```tsx
render(
  <AppShell
    activePrimary="telegram"
    activeSecondary="session"
    expandedPlatform="telegram"
    onTogglePlatform={onTogglePlatform}
    onSelectItem={onSelectItem}
  >
    <div>Content</div>
  </AppShell>
);

await user.click(screen.getByRole("button", { name: /^сессия$/i }));

expect(onSelectItem).toHaveBeenCalledWith({
  primary: "telegram",
  secondary: "session"
});
```

- [ ] **Step 6: Run tests to verify the new contract passes**

Run:

```bash
npm --prefix frontend test -- AppShell.test.tsx App.test.tsx --runInBand
```

Expected:

```text
PASS frontend/src/features/shell/AppShell.test.tsx
PASS frontend/src/app/App.test.tsx
```

- [ ] **Step 7: Commit**

```bash
git add frontend/src/features/shell/navigation.types.ts frontend/src/features/shell/AppShell.test.tsx frontend/src/app/App.test.tsx
git commit -m "test: define platform navigation contract"
```

---

### Task 2: Refactor AppShell Into Accordion Navigation

**Files:**
- Modify: `frontend/src/features/shell/AppShell.tsx`
- Modify: `frontend/src/shared/styles/global.css`
- Test: `frontend/src/features/shell/AppShell.test.tsx`

- [ ] **Step 1: Extend shell tests with active-state and single-expanded-platform behavior**

```tsx
it("renders only the expanded platform subsection list", () => {
  render(
    <AppShell
      activePrimary="vk"
      activeSecondary="groups"
      expandedPlatform="vk"
    >
      <div>Content</div>
    </AppShell>
  );

  expect(screen.getByRole("button", { name: /^группы$/i })).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: /^сессия$/i })).not.toBeInTheDocument();
});

it("marks the selected subsection as current page", () => {
  render(
    <AppShell
      activePrimary="telegram"
      activeSecondary="session"
      expandedPlatform="telegram"
    >
      <div>Content</div>
    </AppShell>
  );

  expect(screen.getByRole("button", { name: /^сессия$/i })).toHaveAttribute(
    "aria-current",
    "page"
  );
});
```

- [ ] **Step 2: Run the shell test file to confirm new failures**

Run:

```bash
npm --prefix frontend test -- AppShell.test.tsx --runInBand
```

Expected:

```text
FAIL frontend/src/features/shell/AppShell.test.tsx
```

- [ ] **Step 3: Replace the flat navigation component with hierarchical rendering**

```tsx
type AppShellProps = PropsWithChildren<{
  activePrimary: PrimarySection;
  activeSecondary: SecondarySection | null;
  expandedPlatform: PlatformSection | null;
  onSelectItem?: (selection: NavigationSelection) => void;
  onTogglePlatform?: (platform: PlatformSection) => void;
}>;

function renderPlatform(platform: PlatformSection) {
  const isExpanded = expandedPlatform === platform;
  const isActive = activePrimary === platform;

  return (
    <section className={isExpanded ? "app-platform is-expanded" : "app-platform"}>
      <button
        type="button"
        className={isActive ? "app-platform__trigger is-active" : "app-platform__trigger"}
        onClick={() => onTogglePlatform?.(platform)}
      >
        <span className="app-platform__title">{platformTitle[platform]}</span>
        <span className="app-platform__meta">{platformStatus[platform]}</span>
      </button>

      {isExpanded ? (
        <div className="app-platform__items">
          {platformMenu[platform].map((secondary) => (
            <button
              key={secondary}
              type="button"
              aria-current={
                activePrimary === platform && activeSecondary === secondary ? "page" : undefined
              }
              onClick={() => onSelectItem?.({ primary: platform, secondary })}
            >
              {secondaryLabels[secondary]}
            </button>
          ))}
        </div>
      ) : null}
    </section>
  );
}
```

- [ ] **Step 4: Add navigation styling for accordion panels and second-level items**

```css
.app-sidebar__nav {
  display: grid;
  gap: 12px;
}

.app-platform {
  border: 1px solid rgba(46, 67, 88, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.45);
  overflow: clip;
}

.app-platform.is-expanded {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(227, 234, 240, 0.92));
  box-shadow: 0 18px 36px rgba(35, 54, 72, 0.12);
}

.app-platform__items {
  display: grid;
  gap: 8px;
  padding: 0 14px 14px;
}

.app-platform__items button[aria-current="page"] {
  background: rgba(40, 67, 92, 0.12);
  border-color: rgba(40, 67, 92, 0.24);
}
```

- [ ] **Step 5: Run the shell tests again**

Run:

```bash
npm --prefix frontend test -- AppShell.test.tsx --runInBand
```

Expected:

```text
PASS frontend/src/features/shell/AppShell.test.tsx
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/features/shell/AppShell.tsx frontend/src/shared/styles/global.css frontend/src/features/shell/AppShell.test.tsx
git commit -m "feat: add platform accordion sidebar"
```

---

### Task 3: Move App State To Primary And Secondary Navigation

**Files:**
- Modify: `frontend/src/app/App.tsx`
- Modify: `frontend/src/app/App.test.tsx`
- Create: `frontend/src/features/shell/PlaceholderPage.tsx`

- [ ] **Step 1: Add app tests for placeholder rendering and preserved subsection selection**

```tsx
it("shows a placeholder for unfinished destinations", async () => {
  window.localStorage.setItem("centranalytics.token", "demo-token");
  vi.spyOn(window, "fetch").mockResolvedValue(
    new Response(JSON.stringify([]), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    })
  );

  render(<App />);

  await userEvent.click(await screen.findByRole("button", { name: /whatsapp/i }));
  await userEvent.click(screen.getByRole("button", { name: /^webhook$/i }));

  expect(await screen.findByRole("heading", { name: /whatsapp/i })).toBeInTheDocument();
  expect(screen.getByText(/экран раздела пока не реализован/i)).toBeInTheDocument();
});
```

- [ ] **Step 2: Run app tests to verify failure**

Run:

```bash
npm --prefix frontend test -- App.test.tsx --runInBand
```

Expected:

```text
FAIL frontend/src/app/App.test.tsx
```

- [ ] **Step 3: Add a reusable placeholder page component**

```tsx
type PlaceholderPageProps = {
  eyebrow: string;
  title: string;
  description: string;
};

export function PlaceholderPage({
  eyebrow,
  title,
  description
}: PlaceholderPageProps) {
  return (
    <section className="placeholder-page">
      <p className="placeholder-page__eyebrow">{eyebrow}</p>
      <h1>{title}</h1>
      <p>{description}</p>
    </section>
  );
}
```

- [ ] **Step 4: Replace flat section state in `App.tsx`**

```tsx
const [activePrimary, setActivePrimary] = useState<PrimarySection>("overview");
const [activeSecondary, setActiveSecondary] = useState<SecondarySection | null>(null);
const [expandedPlatform, setExpandedPlatform] = useState<PlatformSection | null>(null);
const [platformSelections, setPlatformSelections] = useState<
  Partial<Record<PlatformSection, SecondarySection>>
>({});

function handleTogglePlatform(platform: PlatformSection) {
  setExpandedPlatform((current) => (current === platform ? null : platform));
}

function handleSelectItem(selection: NavigationSelection) {
  setActivePrimary(selection.primary);
  setActiveSecondary(selection.secondary);

  if (selection.secondary && isPlatformSection(selection.primary)) {
    setExpandedPlatform(selection.primary);
    setPlatformSelections((current) => ({
      ...current,
      [selection.primary]: selection.secondary
    }));
  }
}
```

- [ ] **Step 5: Implement page selection rules for overview, settings, messages, and placeholders**

```tsx
function renderContent() {
  if (activePrimary === "overview") {
    return (
      <PlaceholderPage
        eyebrow="Операционный обзор"
        title="Выберите раздел платформы"
        description="Обзорный экран появится отдельно, а сейчас отсюда начинается навигация по каналам."
      />
    );
  }

  if (activePrimary === "settings") {
    return (
      <PlaceholderPage
        eyebrow="Система"
        title="Настройки"
        description="Глобальные настройки будут жить отдельно от платформенных блоков."
      />
    );
  }

  return (
    <PlaceholderPage
      eyebrow="Раздел в работе"
      title={`${primaryLabel(activePrimary)} / ${secondaryLabel(activeSecondary)}`}
      description="Экран раздела пока не реализован, но новая структура навигации уже активна."
    />
  );
}
```

- [ ] **Step 6: Run app tests to verify the new state model passes**

Run:

```bash
npm --prefix frontend test -- App.test.tsx --runInBand
```

Expected:

```text
PASS frontend/src/app/App.test.tsx
```

- [ ] **Step 7: Commit**

```bash
git add frontend/src/app/App.tsx frontend/src/app/App.test.tsx frontend/src/features/shell/PlaceholderPage.tsx
git commit -m "feat: move app to platform navigation state"
```

---

### Task 4: Reuse DashboardPage For Platform Message Screens

**Files:**
- Modify: `frontend/src/features/dashboard/DashboardPage.tsx`
- Modify: `frontend/src/app/App.tsx`
- Test: `frontend/src/app/App.test.tsx`

- [ ] **Step 1: Add a failing test for Telegram message view using the existing dashboard**

```tsx
it("renders the dashboard for telegram messages", async () => {
  window.localStorage.setItem("centranalytics.token", "demo-token");
  vi.spyOn(window, "fetch").mockResolvedValue(
    new Response(JSON.stringify([]), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    })
  );

  render(<App />);

  await userEvent.click(await screen.findByRole("button", { name: /телеграм/i }));
  await userEvent.click(screen.getByRole("button", { name: /^сообщения$/i }));

  expect(await screen.findByRole("heading", { name: /сообщения/i })).toBeInTheDocument();
  expect(screen.getByDisplayValue("TELEGRAM")).toBeInTheDocument();
});
```

- [ ] **Step 2: Run the targeted test and confirm failure**

Run:

```bash
npm --prefix frontend test -- App.test.tsx --runInBand
```

Expected:

```text
FAIL frontend/src/app/App.test.tsx
```

- [ ] **Step 3: Add an optional initial platform prop to `DashboardPage`**

```tsx
type DashboardPageProps = {
  token: string;
  initialPlatform?: "ALL" | "TELEGRAM" | "WHATSAPP" | "VK" | "MAX";
  loadMessages?: typeof fetchMessages;
  onUnauthorized?: () => void;
};

export function DashboardPage({
  token,
  initialPlatform = "ALL",
  loadMessages = fetchMessages,
  onUnauthorized
}: DashboardPageProps) {
  const [platform, setPlatform] = useState(initialPlatform);

  useEffect(() => {
    void refresh("", initialPlatform, 0, PAGE_SIZE);
  }, [initialPlatform]);
}
```

- [ ] **Step 4: Wire platform message selections from `App.tsx`**

```tsx
if (activeSecondary === "messages" && activePrimary === "telegram") {
  return (
    <DashboardPage
      token={token}
      initialPlatform="TELEGRAM"
      onUnauthorized={clearSession}
    />
  );
}

if (activeSecondary === "messages" && activePrimary === "vk") {
  return <DashboardPage token={token} initialPlatform="VK" onUnauthorized={clearSession} />;
}

if (activeSecondary === "messages" && activePrimary === "whatsapp") {
  return (
    <DashboardPage
      token={token}
      initialPlatform="WHATSAPP"
      onUnauthorized={clearSession}
    />
  );
}
```

- [ ] **Step 5: Run app tests again**

Run:

```bash
npm --prefix frontend test -- App.test.tsx --runInBand
```

Expected:

```text
PASS frontend/src/app/App.test.tsx
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/features/dashboard/DashboardPage.tsx frontend/src/app/App.tsx frontend/src/app/App.test.tsx
git commit -m "feat: reuse dashboard for platform message views"
```

---

### Task 5: Split Telegram And VK Integration Surfaces Into Platform Pages

**Files:**
- Create: `frontend/src/features/integrations/TelegramSessionPage.tsx`
- Create: `frontend/src/features/integrations/VkGroupsPage.tsx`
- Modify: `frontend/src/features/integrations/IntegrationsPage.tsx`
- Modify: `frontend/src/app/App.tsx`
- Modify: `frontend/src/features/integrations/IntegrationsPage.test.tsx`

- [ ] **Step 1: Add failing tests for platform-specific integration destinations**

```tsx
it("renders the telegram session page independently", async () => {
  render(<TelegramSessionPage token="token" loadSnapshot={vi.fn().mockResolvedValue(snapshot)} />);

  expect(await screen.findByRole("heading", { name: /telegram/i })).toBeInTheDocument();
  expect(screen.getByText("+79991234567")).toBeInTheDocument();
  expect(screen.queryByRole("heading", { name: /интеграции/i })).not.toBeInTheDocument();
});

it("renders the vk groups page independently", async () => {
  render(<VkGroupsPage token="token" loadSnapshot={vi.fn().mockResolvedValue(extendedSnapshot)} />);

  expect(await screen.findByText("Приморский край 24")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: /собрать выбранные/i })).toBeInTheDocument();
});
```

- [ ] **Step 2: Run the integrations test file and confirm failure**

Run:

```bash
npm --prefix frontend test -- IntegrationsPage.test.tsx --runInBand
```

Expected:

```text
FAIL frontend/src/features/integrations/IntegrationsPage.test.tsx
```

- [ ] **Step 3: Extract Telegram session UI into a focused page component**

```tsx
export function TelegramSessionPage(props: IntegrationsPageProps) {
  const {
    telegramSession,
    phoneNumber,
    code,
    password,
    isSubmitting,
    error
  } = useTelegramSessionModel(props);

  return (
    <main className="integrations-shell">
      <section className="integrations-hero">
        <div>
          <p className="integrations-hero__eyebrow">Платформа</p>
          <h1>Telegram</h1>
          <p>Управление пользовательской сессией Telegram без общего экрана интеграций.</p>
        </div>
      </section>

      <article className="integration-panel integration-panel--telegram">
        {/* existing Telegram controls moved here */}
      </article>
    </main>
  );
}
```

- [ ] **Step 4: Extract VK operations UI into a focused page component**

```tsx
export function VkGroupsPage(props: IntegrationsPageProps) {
  const {
    vkGroups,
    selectedVkGroupIds,
    vkActionMessage,
    isVkSubmitting,
    error
  } = useVkGroupsModel(props);

  return (
    <main className="integrations-shell">
      <section className="integrations-hero">
        <div>
          <p className="integrations-hero__eyebrow">Платформа</p>
          <h1>Вконтакте</h1>
          <p>Работа с группами и ручным запуском сбора в отдельном платформенном разделе.</p>
        </div>
      </section>

      <article className="integration-panel integration-panel--vk">
        {/* existing VK group table and actions moved here */}
      </article>
    </main>
  );
}
```

- [ ] **Step 5: Route platform destinations from `App.tsx`**

```tsx
if (activePrimary === "telegram" && activeSecondary === "session") {
  return <TelegramSessionPage token={token} onUnauthorized={clearSession} />;
}

if (
  activePrimary === "vk" &&
  (activeSecondary === "groups" || activeSecondary === "collection")
) {
  return <VkGroupsPage token={token} onUnauthorized={clearSession} />;
}
```

- [ ] **Step 6: Run the integrations and app tests**

Run:

```bash
npm --prefix frontend test -- IntegrationsPage.test.tsx App.test.tsx --runInBand
```

Expected:

```text
PASS frontend/src/features/integrations/IntegrationsPage.test.tsx
PASS frontend/src/app/App.test.tsx
```

- [ ] **Step 7: Commit**

```bash
git add frontend/src/features/integrations/TelegramSessionPage.tsx frontend/src/features/integrations/VkGroupsPage.tsx frontend/src/features/integrations/IntegrationsPage.tsx frontend/src/features/integrations/IntegrationsPage.test.tsx frontend/src/app/App.tsx
git commit -m "feat: split telegram and vk platform pages"
```

---

### Task 6: Add Mobile Drawer Behavior And Run Focused Regression Suite

**Files:**
- Modify: `frontend/src/features/shell/AppShell.tsx`
- Modify: `frontend/src/features/shell/AppShell.test.tsx`
- Modify: `frontend/src/shared/styles/global.css`
- Test: `frontend/src/app/App.test.tsx`
- Test: `frontend/src/features/integrations/IntegrationsPage.test.tsx`

- [ ] **Step 1: Add a failing shell test for mobile navigation toggle**

```tsx
it("opens and closes the mobile navigation drawer", async () => {
  const user = userEvent.setup();

  render(
    <AppShell
      activePrimary="overview"
      activeSecondary={null}
      expandedPlatform={null}
    >
      <div>Content</div>
    </AppShell>
  );

  await user.click(screen.getByRole("button", { name: /открыть навигацию/i }));
  expect(screen.getByRole("navigation", { name: /основная навигация/i })).toHaveClass("is-open");

  await user.click(screen.getByRole("button", { name: /закрыть навигацию/i }));
  expect(screen.getByRole("navigation", { name: /основная навигация/i })).not.toHaveClass("is-open");
});
```

- [ ] **Step 2: Run the shell tests to confirm failure**

Run:

```bash
npm --prefix frontend test -- AppShell.test.tsx --runInBand
```

Expected:

```text
FAIL frontend/src/features/shell/AppShell.test.tsx
```

- [ ] **Step 3: Add local drawer state and responsive shell styling**

```tsx
const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);

<button
  type="button"
  className="app-shell__nav-toggle"
  aria-label="Открыть навигацию"
  onClick={() => setIsMobileNavOpen(true)}
/>

<nav
  aria-label="Основная навигация"
  className={isMobileNavOpen ? "app-sidebar__nav is-open" : "app-sidebar__nav"}
>
  <button
    type="button"
    className="app-shell__nav-close"
    aria-label="Закрыть навигацию"
    onClick={() => setIsMobileNavOpen(false)}
  />
</nav>
```

```css
@media (max-width: 960px) {
  .app-sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    width: min(88vw, 360px);
    transform: translateX(-104%);
    transition: transform 220ms ease;
    z-index: 30;
  }

  .app-sidebar.is-open {
    transform: translateX(0);
  }
}
```

- [ ] **Step 4: Run focused regression tests**

Run:

```bash
npm --prefix frontend test -- AppShell.test.tsx App.test.tsx IntegrationsPage.test.tsx DashboardPage.test.tsx --runInBand
```

Expected:

```text
PASS frontend/src/features/shell/AppShell.test.tsx
PASS frontend/src/app/App.test.tsx
PASS frontend/src/features/integrations/IntegrationsPage.test.tsx
PASS frontend/src/features/dashboard/DashboardPage.test.tsx
```

- [ ] **Step 5: Build the frontend for a final compile check**

Run:

```bash
npm --prefix frontend run build
```

Expected:

```text
vite v...
✓ built in ...
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/features/shell/AppShell.tsx frontend/src/features/shell/AppShell.test.tsx frontend/src/shared/styles/global.css frontend/src/app/App.test.tsx frontend/src/features/integrations/IntegrationsPage.test.tsx
git commit -m "feat: finish responsive platform navigation"
```
