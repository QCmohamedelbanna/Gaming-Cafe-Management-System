import { useLanguage } from "../../i18n";
import { useAuth } from "../../auth/AuthContext";

export default function Header({ title }) {
  const { isArabic, t, toggleLanguage } = useLanguage();
  const { user, logout } = useAuth();

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
          </span>
          <button type="button" className="logout-button" onClick={logout}>Sign out</button>
        </div>
      </div>
    </header>
  );
}
