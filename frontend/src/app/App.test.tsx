import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import App from "./App";

describe("App", () => {
  it("renders the auth heading when there is no token", () => {
    window.localStorage.removeItem("centranalytics.token");

    render(<App />);

    expect(screen.getByRole("heading", { name: /центр аналитики/i })).toBeInTheDocument();
  });

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
    vi.restoreAllMocks();
    window.localStorage.removeItem("centranalytics.token");
  });

  it("clears the saved token and returns to auth after an unauthorized API response", async () => {
    const user = userEvent.setup();
    window.localStorage.setItem("centranalytics.token", "expired-token");
    vi.spyOn(window, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ error: "Invalid or expired token" }), {
        status: 401,
        headers: { "Content-Type": "application/json" }
      })
    );

    render(<App />);

    await user.click(await screen.findByRole("button", { name: /телеграм/i }));
    await user.click(screen.getByRole("button", { name: /^сообщения$/i }));
    expect(await screen.findByRole("heading", { name: /центр аналитики/i })).toBeInTheDocument();
    expect(window.localStorage.getItem("centranalytics.token")).toBeNull();

    vi.restoreAllMocks();
  });
});
