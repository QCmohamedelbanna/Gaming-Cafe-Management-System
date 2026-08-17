import { useState } from "react";
import Sidebar from "./Sidebar";
import Header from "./Header";
import BillExpiryAlert from "../billing/BillExpiryAlert";

export default function Layout({
  activePage,
  onNavigate,
  title,
  children,
}) {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => {
    if (typeof window === "undefined") return false;
    return window.localStorage.getItem("gaming-cafe-sidebar-collapsed") === "true";
  });

  function toggleSidebar() {
    setSidebarCollapsed((current) => {
      const next = !current;
      window.localStorage.setItem("gaming-cafe-sidebar-collapsed", String(next));
      return next;
    });
  }

  return (
    <div className={"app-layout " + (sidebarCollapsed ? "sidebar-is-collapsed" : "")}>
      <Sidebar
        activePage={activePage}
        onNavigate={onNavigate}
        collapsed={sidebarCollapsed}
        onToggle={toggleSidebar}
      />

        <main className={"app-main " + (sidebarCollapsed ? "sidebar-is-collapsed" : "")}>
            <Header title={title} />

            <BillExpiryAlert onNavigate={onNavigate} />

            <div className="app-content">
          {children}
        </div>
      </main>
    </div>
  );
}
