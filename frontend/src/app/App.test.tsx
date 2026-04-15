import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import App from "./App";

const overviewPayload = {
  generatedAt: "2026-04-10T10:15:00Z",
  window: "24h",
  summary: {
    messageCount: 120,
    conversationCount: 18,
    activeAuthorCount: 44,
    platformIssueCount: 1
  },
  platforms: [
    {
      platform: "TELEGRAM",
      label: "Telegram",
      status: "healthy",
      highlights: [{ label: "Сообщения", value: "90" }],
      trend: [{ timestamp: "2026-04-10T09:00:00Z", messageCount: 12 }],
      integration: {
        syncStatus: "healthy",
        sourceCount: 1,
        lastEventAt: "2026-04-10T10:00:00Z",
        lastSuccessAt: "2026-04-10T10:00:00Z",
        lastErrorMessage: null,
        detail: "Сессия активна"
      },
      attentionItems: [{ tone: "healthy", message: "Критичных сигналов не обнаружено." }]
    },
    {
      platform: "VK",
      label: "VK",
      status: "warning",
      highlights: [{ label: "Сообщения", value: "30" }],
      trend: [],
      integration: {
        syncStatus: "warning",
        sourceCount: 1,
        lastEventAt: "2026-04-10T02:00:00Z",
        lastSuccessAt: "2026-04-10T02:00:00Z",
        lastErrorMessage: null,
        detail: "Группы под управлением: 3"
      },
      attentionItems: [{ tone: "warning", message: "Нет новых событий более 6 часов." }]
    },
    {
      platform: "WHATSAPP",
      label: "WhatsApp",
      status: "inactive",
      highlights: [{ label: "Сообщения", value: "0" }],
      trend: [],
      integration: {
        syncStatus: "inactive",
        sourceCount: 0,
        lastEventAt: null,
        lastSuccessAt: null,
        lastErrorMessage: null,
        detail: "Webhook и источники не настроены"
      },
      attentionItems: [{ tone: "idle", message: "Платформа пока не настроена или неактивна." }]
    },
    {
      platform: "MAX",
      label: "Max",
      status: "inactive",
      highlights: [{ label: "Сообщения", value: "0" }],
      trend: [],
      integration: {
        syncStatus: "inactive",
        sourceCount: 0,
        lastEventAt: null,
        lastSuccessAt: null,
        lastErrorMessage: null,
        detail: "Платформа ещё не подключена"
      },
      attentionItems: [{ tone: "idle", message: "Платформа пока не настроена или неактивна." }]
    }
  ]
};

describe("App", () => {
  it("renders the auth heading when there is no token", () => {
    window.localStorage.removeItem("centranalytics.token");

    render(<App />);

    expect(screen.getByRole("heading", { name: /центр аналитики/i })).toBeInTheDocument();
  });

  it("renders overview entry points when there is a saved token", async () => {
    window.localStorage.setItem("centranalytics.token", "demo-token");
    vi.spyOn(window, "fetch").mockResolvedValue(
      new Response(JSON.stringify(overviewPayload), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      })
    );

    render(<App />);

    expect(await screen.findByRole("button", { name: /обзор/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /обзор/i })).toBeInTheDocument();
    expect(screen.getByText("Telegram")).toBeInTheDocument();
    vi.restoreAllMocks();
    window.localStorage.removeItem("centranalytics.token");
  });

  it("clears the saved token and returns to auth after an unauthorized API response", async () => {
    window.localStorage.setItem("centranalytics.token", "expired-token");
    vi.spyOn(window, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ error: "Invalid or expired token" }), {
        status: 401,
        headers: { "Content-Type": "application/json" }
      })
    );

    render(<App />);

    expect(await screen.findByRole("heading", { name: /центр аналитики/i })).toBeInTheDocument();
    expect(window.localStorage.getItem("centranalytics.token")).toBeNull();

    vi.restoreAllMocks();
  });

  it("routes telegram session to a dedicated platform page", async () => {
    const user = userEvent.setup();
    window.localStorage.setItem("centranalytics.token", "demo-token");
    vi.spyOn(window, "fetch").mockImplementation(async (input) => {
      const url = String(input);

      if (url.includes("/api/overview")) {
        return new Response(JSON.stringify(overviewPayload), {
          status: 200,
          headers: { "Content-Type": "application/json" }
        });
      }

      if (url.includes("/api/admin/integrations/telegram-user/current")) {
        return new Response(
          JSON.stringify({
            id: "7",
            phoneNumber: "+79991234567",
            telegramUserId: 10101,
            state: "WAIT_CODE",
            authorized: false,
            errorMessage: null
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

    await screen.findByRole("heading", { name: /обзор/i });
    await user.click(screen.getByRole("button", { name: /телеграм/i }));
    await user.click(screen.getByRole("button", { name: /^сессия$/i }));

    expect(
      await screen.findByRole("heading", { name: /telegram/i, level: 1 })
    ).toBeInTheDocument();
    expect(screen.getByText("+79991234567")).toBeInTheDocument();

    vi.restoreAllMocks();
    window.localStorage.removeItem("centranalytics.token");
  });

  it("routes max sources to a dedicated platform page", async () => {
    const user = userEvent.setup();
    window.localStorage.setItem("centranalytics.token", "demo-token");
    vi.spyOn(window, "fetch").mockResolvedValue(
      new Response(JSON.stringify(overviewPayload), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      })
    );

    render(<App />);

    await screen.findByRole("heading", { name: /обзор/i });
    await user.click(
      screen.getByRole("button", { name: /max.*источники и сообщения платформы max/i })
    );
    await user.click(screen.getByRole("button", { name: /^источники$/i }));

    expect(await screen.findByRole("heading", { name: /max/i, level: 1 })).toBeInTheDocument();
    expect(
      screen.getByText(/read-only webhook ingress для входящих сообщений max через wappi/i)
    ).toBeInTheDocument();
    expect(screen.getByText("/api/integrations/webhooks/wappi/max")).toBeInTheDocument();

    vi.restoreAllMocks();
    window.localStorage.removeItem("centranalytics.token");
  });

  it("routes whatsapp webhook to a dedicated platform page", async () => {
    const user = userEvent.setup();
    window.localStorage.setItem("centranalytics.token", "demo-token");
    vi.spyOn(window, "fetch").mockResolvedValue(
      new Response(JSON.stringify(overviewPayload), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      })
    );

    render(<App />);

    await screen.findByRole("heading", { name: /обзор/i });
    await user.click(
      screen.getByRole("button", {
        name: /whatsapp.*входящий канал, webhook и источники/i
      })
    );
    await user.click(screen.getByRole("button", { name: /^webhook$/i }));

    expect(
      await screen.findByRole("heading", { name: /whatsapp/i, level: 1 })
    ).toBeInTheDocument();
    expect(
      screen.getByText(/операционный экран inbound webhook-канала whatsapp через wappi/i)
    ).toBeInTheDocument();
    expect(screen.getByText("/api/integrations/webhooks/wappi")).toBeInTheDocument();

    vi.restoreAllMocks();
    window.localStorage.removeItem("centranalytics.token");
  });
});
