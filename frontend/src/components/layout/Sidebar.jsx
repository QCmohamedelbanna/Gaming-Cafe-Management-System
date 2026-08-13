import { useLanguage } from "../../i18n";

export default function Sidebar({activePage, onNavigate}) {
    const { t } = useLanguage();
    const items = [
        { key: "operations", label: t("nav.operations") },
        { key: "pos", label: t("nav.pos") },

        // Admin pages later:
        { key: "dashboard", label: t("nav.dashboard") },
        { key: "devices", label: t("nav.devices") },
        { key: "products", label: t("nav.products") },
        { key: "billing", label: t("nav.billing") },
        { key: "inventory", label: t("nav.inventory") },
        { key: "pricing", label: t("nav.pricing") },
        { key: "reports", label: t("nav.reports") },
        { key: "settings", label: t("nav.settings") },
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
                {items.map((item) => (
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
