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

  it("routes telegram session to a dedicated platform page", async () => {
    const user = userEvent.setup();
    window.localStorage.setItem("centranalytics.token", "demo-token");
    vi.spyOn(window, "fetch").mockImplementation(async (input) => {
      const url = String(input);

      if (url.includes("/api/admin/integrations/telegram-user/current")) {
        return new Response(
          JSON.stringify({
            id: "7",
            phoneNumber: "+79991234567",
            telegramUserId: 10101,
            state: "WAIT_CODE",
            authorized: false,
            errorMessage: null,
            lastSyncAt: "2026-04-07T03:10:00Z"
          }),
          {
            status: 200,
            headers: { "Content-Type": "application/json" }
          }
        );
      }

      if (url.includes("/api/admin/integrations/vk/groups")) {
        return new Response(JSON.stringify([]), {
          status: 200,
          headers: { "Content-Type": "application/json" }
        });
      }

      return new Response(JSON.stringify([]), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      });
    });

    render(<App />);

    await user.click(await screen.findByRole("button", { name: /телеграм/i }));
    await user.click(screen.getByRole("button", { name: /^сессия$/i }));

    expect(
      await screen.findByRole("heading", { name: /telegram/i, level: 1 })
    ).toBeInTheDocument();
    expect(screen.getByText("+79991234567")).toBeInTheDocument();

    vi.restoreAllMocks();
    window.localStorage.removeItem("centranalytics.token");
  });
});
