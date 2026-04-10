import type { PropsWithChildren } from "react";

import {
  type NavigationSelection,
  type PlatformSection,
  type PrimarySection,
  type SecondarySection,
  platformMenu
} from "./navigation.types";

type AppShellProps = PropsWithChildren<{
  activePrimary: PrimarySection;
  activeSecondary: SecondarySection | null;
  expandedPlatform: PlatformSection | null;
  onTogglePlatform?: (platform: PlatformSection) => void;
  onSelectItem?: (selection: NavigationSelection) => void;
}>;

const primaryLabels: Record<PrimarySection, string> = {
  overview: "Обзор",
  vk: "Вконтакте",
  telegram: "Телеграм",
  max: "Max",
  whatsapp: "Whatsapp",
  settings: "Настройки"
};

const primaryDescriptions: Record<PrimarySection, string> = {
  overview: "Точка входа в платформенные разделы",
  vk: "Потоки, группы и ручной сбор VK",
  telegram: "Диалоги, сообщения и пользовательская сессия",
  max: "Источники и сообщения платформы Max",
  whatsapp: "Входящий канал, webhook и источники",
  settings: "Глобальные параметры окружения и доступа"
};

const secondaryLabels: Record<SecondarySection, string> = {
  messages: "Сообщения",
  groups: "Группы",
  collection: "Сбор",
  dialogs: "Диалоги",
  session: "Сессия",
  sources: "Источники",
  webhook: "Webhook"
};

function renderTopLevelButton(
  id: PrimarySection,
  activePrimary: PrimarySection,
  onSelectItem?: (selection: NavigationSelection) => void
) {
  const isActive = activePrimary === id;

  return (
    <button
      key={id}
      type="button"
      className={isActive ? "app-nav-item is-active" : "app-nav-item"}
      aria-current={isActive ? "page" : undefined}
      onClick={() => onSelectItem?.({ primary: id, secondary: null })}
    >
      <span className="app-nav-item__label">{primaryLabels[id]}</span>
      <span className="app-nav-item__description">{primaryDescriptions[id]}</span>
    </button>
  );
}

export function AppShell({
  activePrimary,
  activeSecondary,
  expandedPlatform,
  onTogglePlatform,
  onSelectItem,
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
            <p>Операционный контур</p>
          </div>
        </div>

        <nav className="app-sidebar__nav" aria-label="Основная навигация">
          {renderTopLevelButton("overview", activePrimary, onSelectItem)}

          {(Object.keys(platformMenu) as PlatformSection[]).map((platform) => {
            const isExpanded = expandedPlatform === platform;
            const isActive = activePrimary === platform;

            return (
              <section
                key={platform}
                className={isExpanded ? "app-platform is-expanded" : "app-platform"}
              >
                <button
                  type="button"
                  className={isActive ? "app-nav-item is-active" : "app-nav-item"}
                  aria-expanded={isExpanded}
                  onClick={() => onTogglePlatform?.(platform)}
                >
                  <span className="app-nav-item__label">{primaryLabels[platform]}</span>
                  <span className="app-nav-item__description">
                    {primaryDescriptions[platform]}
                  </span>
                </button>

                {isExpanded ? (
                  <div className="app-platform__items">
                    {platformMenu[platform].map((secondary) => {
                      const isCurrent =
                        activePrimary === platform && activeSecondary === secondary;

                      return (
                        <button
                          key={secondary}
                          type="button"
                          className={
                            isCurrent
                              ? "app-platform__item is-active"
                              : "app-platform__item"
                          }
                          aria-current={isCurrent ? "page" : undefined}
                          onClick={() =>
                            onSelectItem?.({
                              primary: platform,
                              secondary
                            })
                          }
                        >
                          {secondaryLabels[secondary]}
                        </button>
                      );
                    })}
                  </div>
                ) : null}
              </section>
            );
          })}

          {renderTopLevelButton("settings", activePrimary, onSelectItem)}
        </nav>

        <div className="app-sidebar__footer">
          <span>Система</span>
          <strong>Единое аналитическое пространство</strong>
        </div>
      </aside>

      <div className="app-content">{children}</div>
    </div>
  );
}
