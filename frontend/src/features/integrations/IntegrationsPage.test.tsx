import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { IntegrationsPage } from "./IntegrationsPage";
import { TelegramSessionPage } from "./TelegramSessionPage";
import { VkGroupsPage } from "./VkGroupsPage";
import type {
  IntegrationsResult,
  TelegramActionResult,
  VkGroupActionResult
} from "./integrations.types";

const snapshot: IntegrationsResult = {
  ok: true,
  data: {
    telegramSession: {
      id: 7,
      phoneNumber: "+79991234567",
      telegramUserId: 10101,
      state: "WAIT_CODE",
      authorized: false,
      errorMessage: null
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

const extendedSnapshot: IntegrationsResult = {
  ok: true,
  data: {
    ...snapshot.data,
    vkGroups: [
      snapshot.data.vkGroups[0],
      {
        id: 2,
        vkGroupId: 1002,
        sourceId: 13,
        screenName: "eao_news",
        name: "ЕАО Новости",
        regionMatchSource: "TEXT",
        collectionMethod: "OFFICIAL_API",
        rawJson: "{}",
        updatedAt: "2026-04-07T03:01:00Z"
      },
      {
        id: 3,
        vkGroupId: 1003,
        sourceId: 14,
        screenName: "birobidzhan_live",
        name: "Биробиджан Live",
        regionMatchSource: "STRUCTURED",
        collectionMethod: "OFFICIAL_API",
        rawJson: "{}",
        updatedAt: "2026-04-07T03:02:00Z"
      },
      {
        id: 4,
        vkGroupId: 1004,
        sourceId: 15,
        screenName: "smidovich_today",
        name: "Смидович Сегодня",
        regionMatchSource: "TEXT",
        collectionMethod: "OFFICIAL_API",
        rawJson: "{}",
        updatedAt: "2026-04-07T03:03:00Z"
      },
      {
        id: 5,
        vkGroupId: 1005,
        sourceId: 16,
        screenName: "leninskoe_feed",
        name: "Ленинское Лента",
        regionMatchSource: "TEXT",
        collectionMethod: "OFFICIAL_API",
        rawJson: "{}",
        updatedAt: "2026-04-07T03:04:00Z"
      },
      {
        id: 6,
        vkGroupId: 1006,
        sourceId: 17,
        screenName: "amurzet_chat",
        name: "Амурзет Чат",
        regionMatchSource: "TEXT",
        collectionMethod: "OFFICIAL_API",
        rawJson: "{}",
        updatedAt: "2026-04-07T03:05:00Z"
      },
      {
        id: 7,
        vkGroupId: 1007,
        sourceId: 18,
        screenName: "obluchye_media",
        name: "Облучье Медиа",
        regionMatchSource: "TEXT",
        collectionMethod: "OFFICIAL_API",
        rawJson: "{}",
        updatedAt: "2026-04-07T03:06:00Z"
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
        loadSnapshot={vi.fn().mockResolvedValue(extendedSnapshot)}
      />
    );

    const row = await screen.findByText("Приморский край 24");
    const groupRow = row.closest(".vk-group-row");

    expect(groupRow).not.toBeNull();
    expect(within(groupRow as HTMLElement).getByText(/official_api/i)).toBeInTheDocument();
    expect(within(groupRow as HTMLElement).getByText(/vk.com\/prim_krai_media/i)).toBeInTheDocument();
    expect(screen.getByText("Облучье Медиа")).toBeInTheDocument();
    expect(screen.getByTestId("vk-group-scroll")).toBeInTheDocument();
  });

  it("runs batch collect and delete actions for selected vk groups", async () => {
    const user = userEvent.setup();
    const loadSnapshot = vi
      .fn<() => Promise<IntegrationsResult>>()
      .mockResolvedValueOnce(extendedSnapshot)
      .mockResolvedValueOnce(extendedSnapshot)
      .mockResolvedValueOnce(extendedSnapshot);
    const collectGroups = vi.fn<() => Promise<VkGroupActionResult>>().mockResolvedValue({
      ok: true,
      message: "Сбор запущен для 2 групп",
      unresolvedIdentifiers: ["missing_group"]
    });
    const deleteGroups = vi.fn<() => Promise<VkGroupActionResult>>().mockResolvedValue({
      ok: true,
      message: "Удалено 2 группы",
      unresolvedIdentifiers: []
    });

    render(
      <IntegrationsPage
        token="token"
        loadSnapshot={loadSnapshot}
        collectVkGroups={collectGroups}
        deleteVkGroups={deleteGroups}
      />
    );

    await screen.findByText("ЕАО Новости");

    const rows = screen.getAllByRole("checkbox", { name: /выбрать группу/i });
    await user.click(rows[0]);
    await user.click(rows[1]);

    await user.click(screen.getByRole("button", { name: /собрать выбранные/i }));

    expect(collectGroups).toHaveBeenCalledWith("token", ["1", "2"]);
    expect(await screen.findByText(/сбор запущен для 2 групп/i)).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /удалить выбранные/i }));

    expect(deleteGroups).toHaveBeenCalledWith("token", ["1", "2"]);
    expect(await screen.findByText(/удалено 2 группы/i)).toBeInTheDocument();
  });

  it("runs row-level vk actions", async () => {
    const user = userEvent.setup();
    const collectGroups = vi.fn<() => Promise<VkGroupActionResult>>().mockResolvedValue({
      ok: true,
      message: "Сбор запущен для 1 группы",
      unresolvedIdentifiers: []
    });
    const deleteGroups = vi.fn<() => Promise<VkGroupActionResult>>().mockResolvedValue({
      ok: true,
      message: "Удалена 1 группа",
      unresolvedIdentifiers: []
    });

    render(
      <IntegrationsPage
        token="token"
        loadSnapshot={vi.fn().mockResolvedValue(extendedSnapshot)}
        collectVkGroups={collectGroups}
        deleteVkGroups={deleteGroups}
      />
    );

    const row = (await screen.findByText("Биробиджан Live")).closest(".vk-group-row");
    expect(row).not.toBeNull();

    await user.click(within(row as HTMLElement).getByRole("button", { name: /собрать/i }));
    expect(collectGroups).toHaveBeenCalledWith("token", ["3"]);

    await user.click(within(row as HTMLElement).getByRole("button", { name: /удалить/i }));
    expect(deleteGroups).toHaveBeenCalledWith("token", ["3"]);
  });

  it("renders the telegram session page independently", async () => {
    render(
      <TelegramSessionPage
        token="token"
        loadSnapshot={vi.fn().mockResolvedValue(snapshot)}
      />
    );

    expect(
      await screen.findByRole("heading", { name: /telegram/i, level: 1 })
    ).toBeInTheDocument();
    expect(screen.getByText("+79991234567")).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: /интеграции/i })).not.toBeInTheDocument();
  });

  it("renders the vk groups page independently", async () => {
    render(
      <VkGroupsPage
        token="token"
        loadSnapshot={vi.fn().mockResolvedValue(extendedSnapshot)}
      />
    );

    expect(await screen.findByText("Приморский край 24")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /собрать выбранные/i })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: /интеграции/i })).not.toBeInTheDocument();
  });
});
