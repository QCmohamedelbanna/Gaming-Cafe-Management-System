import { useState } from "react";

import Layout from "./components/layout/Layout";
import Dashboard from "./components/dashboard/Dashboard";
import PricingPage from "./components/pricing/PricingPage";

export default function App() {
  const [activePage, setActivePage] = useState("dashboard");

  function renderPage() {
    switch (activePage) {
      case "pricing":
        return <PricingPage />;

      case "pos":
        return (
          <div className="placeholder-page">
            <h2>POS</h2>
            <p>Coming soon</p>
          </div>
        );

      case "inventory":
        return (
          <div className="placeholder-page">
            <h2>Inventory</h2>
            <p>Coming soon</p>
          </div>
        );

      case "reports":
        return (
          <div className="placeholder-page">
            <h2>Reports</h2>
            <p>Coming soon</p>
          </div>
        );

      case "settings":
        return (
          <div className="placeholder-page">
            <h2>Settings</h2>
            <p>Coming soon</p>
          </div>
        );

      default:
        return <Dashboard />;
    }
  }

  function getTitle() {
    switch (activePage) {
      case "pricing":
        return "Pricing";
      case "pos":
        return "Point of Sale";
      case "inventory":
        return "Inventory";
      case "reports":
        return "Reports";
      case "settings":
        return "Settings";
      default:
        return "Dashboard";
    }
  }

  return (
    <Layout
      activePage={activePage}
      onNavigate={setActivePage}
      title={getTitle()}
    >
      {renderPage()}
    </Layout>
  );
}