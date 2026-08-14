import { useLanguage } from "../../i18n";
import { useAuth } from "../../auth/AuthContext";

export default function Sidebar({activePage, onNavigate}) {
    const { t } = useLanguage();
    const { hasRole } = useAuth();
    const items = [
        { key: "operations", label: t("nav.operations") },
        { key: "pos", label: t("nav.pos") },

        { key: "dashboard", label: t("nav.dashboard"), roles: ["MANAGER", "ADMIN"] },
        { key: "devices", label: t("nav.devices"), roles: ["ADMIN"] },
        { key: "products", label: t("nav.products"), roles: ["MANAGER", "ADMIN"] },
        { key: "billing", label: t("nav.billing") },
        { key: "inventory", label: t("nav.inventory"), roles: ["MANAGER", "ADMIN"] },
        { key: "pricing", label: t("nav.pricing"), roles: ["MANAGER", "ADMIN"] },
        { key: "reports", label: t("nav.reports"), roles: ["MANAGER", "ADMIN"] },
        { key: "shifts", label: t("nav.shifts") },
        { key: "users", label: t("nav.users"), roles: ["ADMIN"] },
        { key: "settings", label: t("nav.settings"), roles: ["ADMIN"] },
    ];

    return (
        <aside className="sidebar">
            <div className="sidebar-brand">
                <div className="brand-icon">🎮</div>

                <div>
                    <h2>{t("brand.name")}</h2>
                    <span>{t("brand.system")}</span>
                </div>
            </div>

            <nav className="sidebar-nav">
                {items.filter((item) => !item.roles || hasRole(...item.roles)).map((item) => (
                    <button
                        key={item.key}
                        className={`sidebar-item ${
                            activePage === item.key ? "active" : ""
                        }`}
                        onClick={() => onNavigate(item.key)}
                    >
                        {item.label}
                    </button>
                ))}
            </nav>
        </aside>
    );
}
