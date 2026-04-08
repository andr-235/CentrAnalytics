import { useState } from "react";

import { AuthPage } from "../features/auth/AuthPage";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { IntegrationsPage } from "../features/integrations/IntegrationsPage";
import { AppShell } from "../features/shell/AppShell";

type Section = "messages" | "conversations" | "users" | "integrations" | "settings";

export default function App() {
  const [token, setToken] = useState(() =>
    typeof window === "undefined"
      ? ""
      : window.localStorage.getItem("centranalytics.token") ?? ""
  );
  const [activeSection, setActiveSection] = useState<Section>("messages");

  function clearSession() {
    if (typeof window !== "undefined") {
      window.localStorage.removeItem("centranalytics.token");
    }

    setToken("");
    setActiveSection("messages");
  }

  if (!token) {
    return <AuthPage onAuthenticated={setToken} />;
  }

  return (
    <AppShell activeItem={activeSection} onNavigate={setActiveSection}>
      {activeSection === "messages" ? (
        <DashboardPage token={token} onUnauthorized={clearSession} />
      ) : activeSection === "integrations" ? (
        <IntegrationsPage token={token} onUnauthorized={clearSession} />
      ) : (
        <section className="placeholder-page">
          <p className="placeholder-page__eyebrow">Раздел в работе</p>
          <h1>
            {{
              conversations: "Диалоги",
              users: "Пользователи",
              integrations: "Интеграции",
              settings: "Настройки",
              messages: "Сообщения"
            }[activeSection]}
          </h1>
          <p>
            Каркас раздела уже заложен в навигацию. Следующим шагом сюда можно
            подключить реальные таблицы и фильтры.
          </p>
        </section>
      )}
    </AppShell>
  );
}
