import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { AppShell } from "./AppShell";

describe("AppShell", () => {
  it("renders the full navigation map", () => {
    render(<AppShell activeItem="messages"><div>Content</div></AppShell>);

    expect(screen.getByRole("navigation", { name: /основная навигация/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /сообщения/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /диалоги/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /пользователи/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /интеграции/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /настройки/i })).toBeInTheDocument();
  });

  it("marks the active section", () => {
    render(<AppShell activeItem="messages"><div>Content</div></AppShell>);

    expect(screen.getByRole("button", { name: /сообщения/i })).toHaveAttribute(
      "aria-current",
      "page"
    );
  });

  it("calls onNavigate for inactive sections", async () => {
    const user = userEvent.setup();
    const onNavigate = vi.fn();

    render(
      <AppShell activeItem="messages" onNavigate={onNavigate}>
        <div>Content</div>
      </AppShell>
    );

    await user.click(screen.getByRole("button", { name: /диалоги/i }));

    expect(onNavigate).toHaveBeenCalledWith("conversations");
  });
});
