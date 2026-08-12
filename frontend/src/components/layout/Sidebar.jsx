export default function Sidebar({activePage, onNavigate}) {
    const items = [
        { key: "operations", label: "Operations" },
        { key: "pos", label: "POS" },

        // Admin pages later:
        { key: "dashboard", label: "Dashboard" },
        { key: "products", label: "Products" },
        { key: "billing", label: "Billing" },
        { key: "inventory", label: "Inventory" },
        { key: "pricing", label: "Pricing" },
        { key: "reports", label: "Reports" },
        { key: "settings", label: "Settings" },
    ];

    return (
        <aside className="sidebar">
            <div className="sidebar-brand">
                <div className="brand-icon">🎮</div>

                <div>
                    <h2>Gaming Cafe</h2>
                    <span>Management System</span>
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
