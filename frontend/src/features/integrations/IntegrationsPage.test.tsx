import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { IntegrationsPage } from "./IntegrationsPage";
import type { IntegrationsResult, TelegramActionResult } from "./integrations.types";

const snapshot: IntegrationsResult = {
  ok: true,
  data: {
    telegramSession: {
      id: 7,
      phoneNumber: "+79991234567",
      telegramUserId: 10101,
      state: "WAIT_CODE",
      authorized: false,
      errorMessage: null,
      lastSyncAt: "2026-04-07T03:10:00Z"
    },
    vkGroups: [
      {
        id: 1,
        vkGroupId: 1001,
        sourceId: 12,
        screenName: "prim_krai_media",
        name: "Приморский край 24",
        regionMatchSource: "TITLE",
        collectionMethod: "OFFICIAL_API",
        rawJson: "{}",
        updatedAt: "2026-04-07T03:00:00Z"
      }
    ]
  }
};

describe("IntegrationsPage", () => {
  it("renders telegram and vk operational blocks", async () => {
    render(
      <IntegrationsPage
        token="token"
        loadSnapshot={vi.fn().mockResolvedValue(snapshot)}
      />
    );

    expect(await screen.findByRole("heading", { name: /интеграции/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /telegram/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /^vk$/i })).toBeInTheDocument();
    expect(screen.getByText("+79991234567")).toBeInTheDocument();
    expect(screen.getByText("Приморский край 24")).toBeInTheDocument();
  });

  it("submits telegram session actions", async () => {
    const user = userEvent.setup();
    const startSession = vi.fn<() => Promise<TelegramActionResult>>().mockResolvedValue({
      ok: true,
      session: snapshot.data.telegramSession!
    });
    const submitCode = vi.fn<() => Promise<TelegramActionResult>>().mockResolvedValue({
      ok: true,
      session: {
        ...snapshot.data.telegramSession!,
        state: "WAIT_PASSWORD"
      }
    });

    render(
      <IntegrationsPage
        token="token"
        loadSnapshot={vi.fn().mockResolvedValue({
          ...snapshot,
          data: {
            ...snapshot.data,
            telegramSession: null
          }
        })}
        startSession={startSession}
        submitCode={submitCode}
      />
    );

    await screen.findByRole("heading", { name: /интеграции/i });

    await user.type(screen.getByPlaceholderText(/\+79991234567/i), "+79991234567");
    await user.click(screen.getByRole("button", { name: /запустить сессию/i }));

    expect(startSession).toHaveBeenCalledWith("token", "+79991234567");

    await user.type(screen.getByPlaceholderText("12345"), "48291");
    await user.click(screen.getByRole("button", { name: /отправить код/i }));

    expect(submitCode).toHaveBeenCalledWith("token", 7, "48291");
  });

  it("shows regional group rows for vk", async () => {
    render(
      <IntegrationsPage
        token="token"
        loadSnapshot={vi.fn().mockResolvedValue(snapshot)}
      />
    );

    const row = await screen.findByText("Приморский край 24");
    const groupRow = row.closest(".vk-group-row");

    expect(groupRow).not.toBeNull();
    expect(within(groupRow as HTMLElement).getByText(/official_api/i)).toBeInTheDocument();
    expect(within(groupRow as HTMLElement).getByText(/vk.com\/prim_krai_media/i)).toBeInTheDocument();
  });
});
