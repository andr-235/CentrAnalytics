import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { AuthPage } from "./AuthPage";

describe("AuthPage", () => {
  it("switches between login and registration modes", async () => {
    const user = userEvent.setup();
    render(<AuthPage />);

    await user.click(screen.getByRole("tab", { name: /регистрация/i }));

    expect(
      screen.getByRole("button", { name: /создать аккаунт/i })
    ).toBeInTheDocument();
  });

  it("shows inline validation errors before submit", async () => {
    const user = userEvent.setup();
    render(<AuthPage />);

    await user.click(screen.getByRole("button", { name: /войти/i }));

    expect(
      await screen.findByText(/username должен быть от 3 до 50 символов/i)
    ).toBeInTheDocument();
    expect(
      await screen.findByText(/пароль должен быть от 6 до 100 символов/i)
    ).toBeInTheDocument();
  });

  it("renders backend auth errors in an alert region", async () => {
    const user = userEvent.setup();
    const submit = vi.fn().mockResolvedValue({
      ok: false as const,
      fieldErrors: {},
      error: "Invalid username or password"
    });

    render(<AuthPage submitAuth={submit} />);

    await user.type(screen.getByLabelText(/логин/i), "demo-user");
    await user.type(screen.getByLabelText(/пароль/i), "password123");
    await user.click(screen.getByRole("button", { name: /войти/i }));

    expect(
      await screen.findByRole("alert", { name: /auth feedback/i })
    ).toHaveTextContent(/invalid username or password/i);
  });
});
