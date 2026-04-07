import type { PropsWithChildren } from "react";

type NavItemId =
  | "messages"
  | "conversations"
  | "users"
  | "integrations"
  | "settings";

type AppShellProps = PropsWithChildren<{
  activeItem: NavItemId;
  onNavigate?: (item: NavItemId) => void;
}>;

const navItems: Array<{
  id: NavItemId;
  label: string;
  description: string;
}> = [
  {
    id: "messages",
    label: "Сообщения",
    description: "Операционный журнал входящего потока"
  },
  {
    id: "conversations",
    label: "Диалоги",
    description: "Структура каналов и цепочек коммуникации"
  },
  {
    id: "users",
    label: "Пользователи",
    description: "Внешние авторы и контактные профили"
  },
  {
    id: "integrations",
    label: "Интеграции",
    description: "Подключенные источники и каналы доставки"
  },
  {
    id: "settings",
    label: "Настройки",
    description: "Параметры окружения и доступа"
  }
];

export function AppShell({
  activeItem,
  onNavigate,
  children
}: AppShellProps) {
  return (
    <div className="app-shell">
      <aside className="app-sidebar">
        <div className="app-sidebar__brand">
          <div className="app-sidebar__mark" aria-hidden="true">
            <span />
          </div>
          <div>
            <strong>Центр аналитики</strong>
            <p>Control surface</p>
          </div>
        </div>

        <nav className="app-sidebar__nav" aria-label="Основная навигация">
          {navItems.map((item) => {
            const isActive = item.id === activeItem;

            return (
              <button
                key={item.id}
                type="button"
                className={isActive ? "app-nav-item is-active" : "app-nav-item"}
                aria-current={isActive ? "page" : undefined}
                onClick={() => {
                  if (!isActive) {
                    onNavigate?.(item.id);
                  }
                }}
              >
                <span className="app-nav-item__label">{item.label}</span>
                <span className="app-nav-item__description">{item.description}</span>
              </button>
            );
          })}
        </nav>

        <div className="app-sidebar__footer">
          <span>System ready</span>
          <strong>Cold steel workspace</strong>
        </div>
      </aside>

      <div className="app-content">{children}</div>
    </div>
  );
}
