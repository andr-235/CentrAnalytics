import { useState } from "react";

import { AuthPage } from "../features/auth/AuthPage";
import { DashboardPage } from "../features/dashboard/DashboardPage";

export default function App() {
  const [token, setToken] = useState(() =>
    typeof window === "undefined"
      ? ""
      : window.localStorage.getItem("centranalytics.token") ?? ""
  );

  if (!token) {
    return <AuthPage onAuthenticated={setToken} />;
  }

  return <DashboardPage token={token} />;
}
