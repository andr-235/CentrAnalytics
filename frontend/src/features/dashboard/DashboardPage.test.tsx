import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { DashboardPage } from "./DashboardPage";
import type { MessageRecord } from "./dashboard.types";

const messages: MessageRecord[] = [
  {
    id: 12,
    platform: "TELEGRAM",
    externalMessageId: "msg-12",
    conversationId: 701,
    authorId: 44,
    text: "Проверка аналитического пайплайна по воронке обращений",
    messageType: "TEXT",
    sentAt: "2026-04-07T02:15:00Z"
  },
  {
    id: 13,
    platform: "WHATSAPP",
    externalMessageId: "msg-13",
    conversationId: 702,
    authorId: 45,
    text: "Нужен повторный обзвон группы риска",
    messageType: "TEXT",
    sentAt: "2026-04-07T03:20:00Z"
  }
];

describe("DashboardPage", () => {
  it("renders message rows from the backend", async () => {
    const loadMessages = vi.fn().mockResolvedValue({
      ok: true as const,
      items: messages
    });

    render(<DashboardPage token="token" loadMessages={loadMessages} />);

    expect(await screen.findByText(/проверка аналитического пайплайна/i)).toBeInTheDocument();
    expect(screen.getByText(/нужен повторный обзвон/i)).toBeInTheDocument();
  });

  it("submits search filters to the loader", async () => {
    const user = userEvent.setup();
    const loadMessages = vi.fn().mockResolvedValue({
      ok: true as const,
      items: messages
    });

    render(<DashboardPage token="token" loadMessages={loadMessages} />);

    await screen.findByText(/проверка аналитического пайплайна/i);
    await user.type(screen.getByPlaceholderText(/поиск по тексту/i), "воронка");
    await user.selectOptions(screen.getByLabelText(/платформа/i), "TELEGRAM");
    await user.click(screen.getByRole("button", { name: /обновить/i }));

    expect(loadMessages).toHaveBeenLastCalledWith("token", {
      search: "воронка",
      platform: "TELEGRAM"
    });
  });

  it("shows a compact error banner when loading fails", async () => {
    const loadMessages = vi.fn().mockResolvedValue({
      ok: false as const,
      error: "Invalid or expired token"
    });

    render(<DashboardPage token="token" loadMessages={loadMessages} />);

    expect(await screen.findByRole("alert")).toHaveTextContent(/invalid or expired token/i);
  });

  it("renders a structured table header", async () => {
    const loadMessages = vi.fn().mockResolvedValue({
      ok: true as const,
      items: messages
    });

    render(<DashboardPage token="token" loadMessages={loadMessages} />);

    const table = await screen.findByRole("table", { name: /журнал сообщений/i });
    const headers = within(table).getAllByRole("columnheader").map((node) => node.textContent);

    expect(headers).toEqual([
      "Платформа",
      "Автор",
      "Сообщение",
      "Диалог",
      "Тип",
      "Время"
    ]);
  });
});
