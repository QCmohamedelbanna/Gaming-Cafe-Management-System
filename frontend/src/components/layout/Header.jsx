import { useState } from "react";
import { useLanguage } from "../../i18n";
import { useAuth } from "../../auth/AuthContext";

export default function Header({ title }) {
  const { isArabic, t, toggleLanguage } = useLanguage();
  const { user, currentShift, logout } = useAuth();
  const [logoutError, setLogoutError] = useState("");

  async function handleLogout() {
    setLogoutError("");
    try {
      await logout();
    } catch (error) {
      setLogoutError(error.message || "Could not sign out");
    }
  }

  return (
    <header className="app-header">
      <div>
        <span className="header-label">{t("header.controlCenter")}</span>
        <h1>{title}</h1>
      </div>

      <div className="header-actions">
        <button
          type="button"
          className="language-switcher"
          onClick={toggleLanguage}
          aria-label={isArabic ? t("header.switchToEnglish") : t("header.switchToArabic")}
          title={isArabic ? t("header.switchToEnglish") : t("header.switchToArabic")}
        >
          <span className="language-switcher-code">{isArabic ? "EN" : "ع"}</span>
          <span>{isArabic ? t("language.english") : t("language.arabic")}</span>
        </button>

        <div className="header-user">
          <span>
            <strong>{user?.displayName || user?.username}</strong>
            <small>{user?.role}</small>
            {currentShift && <small className="logout-hint">Close the open shift before signing out</small>}
            {logoutError && <small className="logout-error">{logoutError}</small>}
          </span>
          <button
            type="button"
            className="logout-button"
            onClick={handleLogout}
            disabled={Boolean(currentShift)}
            title={currentShift ? "Close the open shift before signing out" : "Sign out"}
          >
            Sign out
          </button>
        </div>
      </div>
    </header>
  );
}
