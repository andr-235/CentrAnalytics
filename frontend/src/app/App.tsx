import { useState } from "react";

import { AuthPage } from "../features/auth/AuthPage";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { TelegramSessionPage } from "../features/integrations/TelegramSessionPage";
import { VkGroupsPage } from "../features/integrations/VkGroupsPage";
import { AppShell } from "../features/shell/AppShell";
import type {
  NavigationSelection,
  PlatformSection,
  PrimarySection,
  SecondarySection
} from "../features/shell/navigation.types";

function isPlatformSection(value: PrimarySection): value is PlatformSection {
  return value === "vk" || value === "telegram" || value === "max" || value === "whatsapp";
}

function primaryTitle(value: PrimarySection) {
  switch (value) {
    case "overview":
      return "Обзор";
    case "vk":
      return "Вконтакте";
    case "telegram":
      return "Телеграм";
    case "max":
      return "Max";
    case "whatsapp":
      return "Whatsapp";
    case "settings":
      return "Настройки";
  }
}

function secondaryTitle(value: SecondarySection | null) {
  switch (value) {
    case "messages":
      return "Сообщения";
    case "groups":
      return "Группы";
    case "collection":
      return "Сбор";
    case "dialogs":
      return "Диалоги";
    case "session":
      return "Сессия";
    case "sources":
      return "Источники";
    case "webhook":
      return "Webhook";
    default:
      return "";
  }
}

export default function App() {
  const [token, setToken] = useState(() =>
    typeof window === "undefined"
      ? ""
      : window.localStorage.getItem("centranalytics.token") ?? ""
  );
  const [activePrimary, setActivePrimary] = useState<PrimarySection>("overview");
  const [activeSecondary, setActiveSecondary] = useState<SecondarySection | null>(null);
  const [expandedPlatform, setExpandedPlatform] = useState<PlatformSection | null>(null);

  function clearSession() {
    if (typeof window !== "undefined") {
      window.localStorage.removeItem("centranalytics.token");
    }

    setToken("");
    setActivePrimary("overview");
    setActiveSecondary(null);
    setExpandedPlatform(null);
  }

  function handleTogglePlatform(platform: PlatformSection) {
    setExpandedPlatform((current) => (current === platform ? null : platform));
  }

  function handleSelectItem(selection: NavigationSelection) {
    setActivePrimary(selection.primary);
    setActiveSecondary(selection.secondary);

    if (isPlatformSection(selection.primary)) {
      setExpandedPlatform(selection.primary);
      return;
    }

    setExpandedPlatform(null);
  }

  function renderContent() {
    if (activePrimary === "overview") {
      return (
        <section className="placeholder-page">
          <p className="placeholder-page__eyebrow">Операционный обзор</p>
          <h1>Выберите раздел платформы</h1>
          <p>
            Навигация теперь организована по каналам. Раскройте нужную платформу
            слева и выберите рабочий подраздел.
          </p>
        </section>
      );
    }

    if (activePrimary === "settings") {
      return (
        <section className="placeholder-page">
          <p className="placeholder-page__eyebrow">Система</p>
          <h1>Настройки</h1>
          <p>Глобальные параметры будут жить отдельно от платформенных разделов.</p>
        </section>
      );
    }

    if (activeSecondary === "messages") {
      return <DashboardPage token={token} onUnauthorized={clearSession} />;
    }

    if (activePrimary === "telegram" && activeSecondary === "session") {
      return <TelegramSessionPage token={token} onUnauthorized={clearSession} />;
    }

    if (activePrimary === "vk" && (activeSecondary === "groups" || activeSecondary === "collection")) {
      return <VkGroupsPage token={token} onUnauthorized={clearSession} />;
    }

    return (
      <section className="placeholder-page">
        <p className="placeholder-page__eyebrow">Раздел в работе</p>
        <h1>{`${primaryTitle(activePrimary)} / ${secondaryTitle(activeSecondary)}`}</h1>
        <p>
          Экран еще не реализован, но новая платформенная структура навигации уже
          активна.
        </p>
      </section>
    );
  }

  if (!token) {
    return <AuthPage onAuthenticated={setToken} />;
  }

  return (
    <AppShell
      activePrimary={activePrimary}
      activeSecondary={activeSecondary}
      expandedPlatform={expandedPlatform}
      onTogglePlatform={handleTogglePlatform}
      onSelectItem={handleSelectItem}
    >
      {renderContent()}
    </AppShell>
  );
}
