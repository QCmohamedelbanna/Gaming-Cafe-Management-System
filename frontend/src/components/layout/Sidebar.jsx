import { useLanguage } from "../../i18n";
import { useAuth } from "../../auth/AuthContext";

export default function Sidebar({activePage, onNavigate, collapsed, onToggle}) {
    const { t } = useLanguage();
    const { hasPermission } = useAuth();
    const items = [
        { key: "operations", label: t("nav.operations"), permission: "OPERATIONS_USE" },
        { key: "reservations", label: t("nav.reservations"), permission: "RESERVATIONS_MANAGE" },
        { key: "pos", label: t("nav.pos"), permission: "POS_USE" },
        { key: "dashboard", label: t("nav.dashboard"), permission: "DASHBOARD_VIEW" },
        { key: "devices", label: t("nav.devices"), permission: "DEVICES_MANAGE" },
        { key: "products", label: t("nav.products"), permission: "PRODUCTS_MANAGE" },
        { key: "billing", label: t("nav.billing"), permission: "CHECKOUT_USE" },
        { key: "inventory", label: t("nav.inventory"), permission: "INVENTORY_MANAGE" },
        { key: "pricing", label: t("nav.pricing"), permission: "PRICING_MANAGE" },
        { key: "reports", label: t("nav.reports"), permission: "REPORTS_VIEW" },
        { key: "shifts", label: t("nav.shifts"), permission: "SHIFT_MANAGE" },
        { key: "users", label: t("nav.users"), permission: "USERS_MANAGE" },
        { key: "permissions", label: t("nav.permissions"), permission: "PERMISSIONS_MANAGE" },
        { key: "rules", label: t("nav.rules"), permission: "PERMISSIONS_MANAGE" },
        { key: "settings", label: t("nav.settings"), permission: "SETTINGS_MANAGE" },
    ];

    return (
        <aside className={"sidebar " + (collapsed ? "collapsed" : "")}>
            <div className="sidebar-brand">
                <div className="brand-icon">🎮</div>

                <div className="sidebar-brand-copy">
                    <h2>{t("brand.name")}</h2>
                    <span>{t("brand.system")}</span>
                </div>
                <button
                    type="button"
                    className="sidebar-toggle"
                    onClick={onToggle}
                    aria-label={collapsed ? "Expand navigation menu" : "Collapse navigation menu"}
                    aria-expanded={!collapsed}
                    title={collapsed ? "Expand navigation menu" : "Collapse navigation menu"}
                >
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path d={collapsed ? "m9 18 6-6-6-6" : "m15 18-6-6 6-6"} />
                    </svg>
                </button>
            </div>

            <nav className="sidebar-nav" aria-label="Sidebar navigation">
                {items.filter((item) => !item.permission || hasPermission(item.permission)).map((item) => (
                    <button
                        key={item.key}
                        className={`sidebar-item ${
                            activePage === item.key ? "active" : ""
                        }`}
                        onClick={() => onNavigate(item.key)}
                    >
                        <span className="sidebar-item-label">{item.label}</span>
                        <span className="sidebar-item-short" aria-hidden="true">{item.label.slice(0, 2).toUpperCase()}</span>
                    </button>
                ))}
            </nav>
        </aside>
    );
}
