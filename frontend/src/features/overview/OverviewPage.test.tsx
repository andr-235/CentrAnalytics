import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { OverviewPage } from "./OverviewPage";

const snapshot = {
  generatedAt: "2026-04-10T10:15:00Z",
  window: "24h" as const,
  summary: {
    messageCount: 120,
    conversationCount: 18,
    activeAuthorCount: 44,
    platformIssueCount: 1
  },
  platforms: [
    {
      platform: "TELEGRAM" as const,
      label: "Telegram",
      status: "healthy" as const,
      highlights: [
        { label: "Сообщения", value: "90" },
        { label: "Диалоги", value: "12" }
      ],
      trend: [
        { timestamp: "2026-04-10T08:00:00Z", messageCount: 12 },
        { timestamp: "2026-04-10T09:00:00Z", messageCount: 18 }
      ],
      integration: {
        syncStatus: "healthy",
        lastEventAt: "2026-04-10T10:00:00Z",
        lastSuccessAt: "2026-04-10T10:00:00Z",
        lastErrorMessage: null,
        sourceCount: 1,
        detail: "Сессия активна"
      },
      attentionItems: [{ tone: "healthy", message: "Критичных сигналов не обнаружено." }]
    },
    {
      platform: "MAX" as const,
      label: "Max",
      status: "inactive" as const,
      highlights: [{ label: "Сообщения", value: "0" }],
      trend: [],
      integration: {
        syncStatus: "inactive",
        lastEventAt: null,
        lastSuccessAt: null,
        lastErrorMessage: null,
        sourceCount: 0,
        detail: "Платформа ещё не подключена"
      },
      attentionItems: [{ tone: "idle", message: "Платформа пока не настроена или неактивна." }]
    }
  ]
};

describe("OverviewPage", () => {
  it("renders summary cards and platform sections for the selected window", async () => {
    const loadOverview = vi.fn().mockResolvedValue({
      ok: true as const,
      data: snapshot
    });

    render(<OverviewPage token="token" loadOverview={loadOverview} />);

    expect(await screen.findByRole("heading", { name: /обзор/i })).toBeInTheDocument();
    expect(screen.getByText("120")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /7д/i })).toBeInTheDocument();
    expect(screen.getByText("Telegram")).toBeInTheDocument();
    expect(screen.getByText("Max")).toBeInTheDocument();
    expect(loadOverview).toHaveBeenCalledWith("token", "24h");
  });

  it("switches the selected window and reloads the snapshot", async () => {
    const user = userEvent.setup();
    const loadOverview = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true as const,
        data: snapshot
      })
      .mockResolvedValueOnce({
        ok: true as const,
        data: {
          ...snapshot,
          window: "7d" as const,
          summary: { ...snapshot.summary, messageCount: 310 }
        }
      });

    render(<OverviewPage token="token" loadOverview={loadOverview} />);

    await screen.findByText("Telegram");
    await user.click(screen.getByRole("button", { name: /7д/i }));

    await waitFor(() => expect(loadOverview).toHaveBeenLastCalledWith("token", "7d"));
    expect(await screen.findByText("310")).toBeInTheDocument();
  });

  it("renders a page-level error when loading fails", async () => {
    const loadOverview = vi.fn().mockResolvedValue({
      ok: false as const,
      unauthorized: false as const,
      error: "Overview is unavailable"
    });

    render(<OverviewPage token="token" loadOverview={loadOverview} />);

    expect(await screen.findByRole("alert")).toHaveTextContent(/overview is unavailable/i);
  });
});
