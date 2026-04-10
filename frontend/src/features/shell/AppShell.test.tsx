import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { AppShell } from "./AppShell";
import type { NavigationSelection } from "./navigation.types";

describe("AppShell", () => {
  it("renders the platform-first navigation map", () => {
    render(
      <AppShell activePrimary="telegram" activeSecondary="messages" expandedPlatform="telegram">
        <div>Content</div>
      </AppShell>
    );

    expect(screen.getByRole("navigation", { name: /основная навигация/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /обзор/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /вконтакте/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /телеграм/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /max/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /whatsapp/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /настройки/i })).toBeInTheDocument();
  });

  it("toggles a platform without selecting a nested item", async () => {
    const user = userEvent.setup();
    const onTogglePlatform = vi.fn();
    const onSelectItem = vi.fn();

    render(
      <AppShell
        activePrimary="overview"
        activeSecondary={null}
        expandedPlatform="vk"
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

  it("selects a nested platform section", async () => {
    const user = userEvent.setup();
    const onSelectItem = vi.fn<[NavigationSelection], void>();

    render(
      <AppShell
        activePrimary="telegram"
        activeSecondary="session"
        expandedPlatform="telegram"
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
  });

  it("renders only the expanded platform subsection list", () => {
    render(
      <AppShell activePrimary="vk" activeSecondary="groups" expandedPlatform="vk">
        <div>Content</div>
      </AppShell>
    );

    expect(screen.getByRole("button", { name: /^группы$/i })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /^сессия$/i })).not.toBeInTheDocument();
  });

  it("marks the selected subsection as the current page", () => {
    render(
      <AppShell activePrimary="telegram" activeSecondary="session" expandedPlatform="telegram">
        <div>Content</div>
      </AppShell>
    );

    expect(screen.getByRole("button", { name: /^сессия$/i })).toHaveAttribute(
      "aria-current",
      "page"
    );
  });
});
