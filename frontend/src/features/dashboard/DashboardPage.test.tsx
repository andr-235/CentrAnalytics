import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { DashboardPage } from "./DashboardPage";
import type { MessageRecord } from "./dashboard.types";

const messages: MessageRecord[] = [
  {
    id: 12,
    platform: "TELEGRAM",
    externalMessageId: "msg-12",
    conversationId: 701,
    conversationTitle: "Оперативный штаб",
    externalConversationId: "chat-701",
    conversationType: "GROUP",
    authorId: 44,
    authorDisplayName: "Алексей Смирнов",
    authorUsername: "alex-smirnov",
    authorExternalUserId: "675752815",
    authorPhone: "+79990001122",
    text: "Проверка аналитического пайплайна по воронке обращений",
    messageType: "TEXT",
    sentAt: "2026-04-07T02:15:00Z"
  },
  {
    id: 13,
    platform: "WHATSAPP",
    externalMessageId: "msg-13",
    conversationId: 702,
    conversationTitle: "Екатерина Волкова",
    externalConversationId: "120363303652035579@g.us",
    conversationType: "GROUP",
    authorId: 45,
    authorDisplayName: "Екатерина Волкова",
    authorUsername: null,
    authorExternalUserId: null,
    authorPhone: "+79990002233",
    text: "Нужен повторный обзвон группы риска",
    messageType: "TEXT",
    sentAt: "2026-04-07T03:20:00Z"
  }
];

function buildPage(size: number, startIndex = 0) {
  return Array.from({ length: size }, (_, index) => ({
    ...messages[0],
    id: messages[0].id + startIndex + index,
    externalMessageId: `msg-page-${startIndex + index}`,
    text: `Сообщение страницы ${startIndex + index}`,
    sentAt: `2026-04-07T02:${String((startIndex + index) % 60).padStart(2, "0")}:00Z`
  }));
}

describe("DashboardPage", () => {
  it("renders message rows from the backend", async () => {
    const loadMessages = vi.fn().mockResolvedValue({
      ok: true as const,
      items: messages
    });

    render(
      <DashboardPage
        token="token"
        initialPlatform="TELEGRAM"
        loadMessages={loadMessages}
      />
    );

    expect(await screen.findByText(/проверка аналитического пайплайна/i)).toBeInTheDocument();
    expect(screen.getByText(/нужен повторный обзвон/i)).toBeInTheDocument();
    expect(loadMessages).toHaveBeenCalledWith("token", {
      limit: 25,
      offset: 0,
      platform: "TELEGRAM",
      search: ""
    });
  });

  it("submits search text to the loader without rendering a platform filter", async () => {
    const user = userEvent.setup();
    const loadMessages = vi.fn().mockResolvedValue({
      ok: true as const,
      items: messages
    });

    render(
      <DashboardPage
        token="token"
        initialPlatform="TELEGRAM"
        loadMessages={loadMessages}
      />
    );

    await screen.findByText(/проверка аналитического пайплайна/i);
    await user.type(screen.getByPlaceholderText(/поиск по тексту/i), "воронка");
    await user.click(screen.getByRole("button", { name: /обновить/i }));

    expect(screen.queryByLabelText(/платформа/i)).not.toBeInTheDocument();
    expect(loadMessages).toHaveBeenLastCalledWith("token", {
      limit: 25,
      offset: 0,
      search: "воронка",
      platform: "TELEGRAM"
    });
  });

  it("moves to the next page", async () => {
    const user = userEvent.setup();
    const firstPage = buildPage(25);
    const secondPage = buildPage(25, 25);
    const loadMessages = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true as const,
        items: firstPage
      })
      .mockResolvedValueOnce({
        ok: true as const,
        items: secondPage
      });

    render(
      <DashboardPage
        token="token"
        initialPlatform="TELEGRAM"
        loadMessages={loadMessages}
      />
    );

    expect(await screen.findByText("Сообщение страницы 0")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /вперёд/i }));

    expect(loadMessages).toHaveBeenLastCalledWith("token", {
      limit: 25,
      offset: 25,
      platform: "TELEGRAM",
      search: ""
    });
    expect(await screen.findByText("Сообщение страницы 25")).toBeInTheDocument();
    expect(screen.queryByText("Сообщение страницы 0")).not.toBeInTheDocument();
    expect(screen.getByText("Страница 2")).toBeInTheDocument();
  });

  it("disables forward navigation when the backend returned less than a page", async () => {
    const loadMessages = vi.fn().mockResolvedValue({
      ok: true as const,
      items: messages
    });

    render(
      <DashboardPage
        token="token"
        initialPlatform="TELEGRAM"
        loadMessages={loadMessages}
      />
    );

    expect(await screen.findByText("Алексей Смирнов")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /вперёд/i })).toBeDisabled();
    expect(screen.getByText("Страница 1")).toBeInTheDocument();
  });

  it("changes page size and resets paging to the first page", async () => {
    const user = userEvent.setup();
    const loadMessages = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true as const,
        items: buildPage(25)
      })
      .mockResolvedValueOnce({
        ok: true as const,
        items: buildPage(50)
      });

    render(
      <DashboardPage
        token="token"
        initialPlatform="TELEGRAM"
        loadMessages={loadMessages}
      />
    );

    await screen.findByText("Сообщение страницы 0");
    await user.selectOptions(screen.getByLabelText(/размер страницы/i), "50");

    await waitFor(() =>
      expect(loadMessages).toHaveBeenLastCalledWith("token", {
        limit: 50,
        offset: 0,
        platform: "TELEGRAM",
        search: ""
      })
    );
    expect(screen.getByText("Страница 1")).toBeInTheDocument();
  });

  it("shows a compact error banner when loading fails", async () => {
    const loadMessages = vi.fn().mockResolvedValue({
      ok: false as const,
      error: "Invalid or expired token"
    });

    render(
      <DashboardPage
        token="token"
        initialPlatform="TELEGRAM"
        loadMessages={loadMessages}
      />
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(/invalid or expired token/i);
  });

  it("renders a platform-specific table header without the platform column", async () => {
    const loadMessages = vi.fn().mockResolvedValue({
      ok: true as const,
      items: messages
    });

    render(
      <DashboardPage
        token="token"
        initialPlatform="TELEGRAM"
        loadMessages={loadMessages}
      />
    );

    const table = await screen.findByRole("table", { name: /журнал сообщений/i });
    const headers = within(table).getAllByRole("columnheader").map((node) => node.textContent);

    expect(headers).toEqual([
      "Автор",
      "Сообщение",
      "Диалог",
      "Тип",
      "Время"
    ]);
  });

  it("renders telegram author username and external id together with the dialog title", async () => {
    const loadMessages = vi.fn().mockResolvedValue({
      ok: true as const,
      items: messages
    });

    render(
      <DashboardPage
        token="token"
        initialPlatform="TELEGRAM"
        loadMessages={loadMessages}
      />
    );

    expect(await screen.findByText("Алексей Смирнов")).toBeInTheDocument();
    expect(screen.getByText("@alex-smirnov · id 675752815")).toBeInTheDocument();
    expect(screen.getByText("Оперативный штаб")).toBeInTheDocument();
    expect(screen.getByText("GROUP · chat-701")).toBeInTheDocument();
    expect(screen.getByText("120363303652035579@g.us")).toBeInTheDocument();
  });

  it("avoids duplicating the conversation title when it matches the author", async () => {
    const loadMessages = vi.fn().mockResolvedValue({
      ok: true as const,
      items: [messages[1]]
    });

    render(
      <DashboardPage
        token="token"
        initialPlatform="WHATSAPP"
        loadMessages={loadMessages}
      />
    );

    expect(await screen.findByText("Екатерина Волкова")).toBeInTheDocument();
    expect(screen.getByText("GROUP")).toBeInTheDocument();
    expect(screen.getByText("120363303652035579@g.us")).toBeInTheDocument();
  });
});
