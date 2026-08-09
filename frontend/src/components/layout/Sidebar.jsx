export default function Sidebar({ activePage, onNavigate }) {
  const items = [
    { key: "dashboard", label: "Dashboard" },
    { key: "pricing", label: "Pricing" },
    { key: "pos", label: "POS" },
    { key: "inventory", label: "Inventory" },
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