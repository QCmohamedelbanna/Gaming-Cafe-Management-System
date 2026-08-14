import { useState } from "react";
import { useLanguage } from "./i18n";
import { useAuth } from "./auth/AuthContext";

import Layout from "./components/layout/Layout";
import LoginPage from "./components/auth/LoginPage";
import Dashboard from "./components/dashboard/Dashboard";
import AdminDashboardPage from "./components/dashboard/AdminDashboardPage";
import PricingPage from "./components/pricing/PricingPage";
import POSPage from "./components/pos/POSPage";
import ProductsPage from "./components/products/ProductsPage";
import BillingPage from "./components/billing/BillingPage";
import DevicesPage from "./components/devices/DevicesPage";
import InventoryPage from "./components/inventory/InventoryPage";
import ReportsPage from "./components/reports/ReportsPage";
import ShiftPage from "./components/shifts/ShiftPage";
import UsersPage from "./components/users/UsersPage";

export default function App() {
    const { t } = useLanguage();
    const { user, loading, hasRole } = useAuth();

    const [activePage, setActivePage] =
        useState("operations");

    /*
     * Session currently attached to POS.
     *
     * null = standalone sale
     */
    const [posSession, setPosSession] =
        useState(null);

    if (loading) {
        return <div className="auth-loading">Loading secure workspace…</div>;
    }

    if (!user) {
        return <LoginPage />;
    }

    function canAccessPage(page) {
        if (["operations", "pos", "billing", "shifts"].includes(page)) return true;
        if (["dashboard", "products", "pricing", "inventory", "reports"].includes(page)) {
            return hasRole("MANAGER", "ADMIN");
        }
        if (page === "devices" || page === "users" || page === "settings") {
            return hasRole("ADMIN");
        }
        return false;
    }


    /*
     * Normal sidebar navigation
     */
    function handleNavigate(page) {
        if (!canAccessPage(page)) return;
        setActivePage(page);
    }


    /*
     * Called from DeviceCard → Add Order
     */
    function handleAddOrder(session) {

        setPosSession(session);

        setActivePage("pos");
    }


    function renderPage() {

        if (!canAccessPage(activePage)) {
            return <Dashboard onAddOrder={handleAddOrder} />;
        }

        switch (activePage) {

            case "operations":
                return (
                    <Dashboard
                        onAddOrder={handleAddOrder}
                    />
                );

            case "dashboard":
                return <AdminDashboardPage />;

            case "pricing":
                return <PricingPage />;

            case "products":
                return <ProductsPage />;

            case "devices":
                return <DevicesPage />;

            case "billing":
                return <BillingPage />;

            case "pos":
                return (
                    <POSPage
                        key={
                            posSession?.id
                            ?? "standalone"
                        }
                        attachedSession={
                            posSession
                        }
                        onStartStandalone={() => setPosSession(null)}
                    />
                );

            case "inventory":
                return <InventoryPage />;

            case "reports":
                return <ReportsPage />;

            case "shifts":
                return <ShiftPage />;

            case "users":
                return <UsersPage />;

            case "settings":
                return (
                    <div className="placeholder-page">
                        <h2>{t("page.settings")}</h2>
                        <p>{t("settings.comingSoon")}</p>
                    </div>
                );

            default:
                return (
                    <Dashboard
                        onAddOrder={
                            handleAddOrder
                        }
                    />
                );
        }
    }


    function getTitle() {
        const titleKeys = {
            operations: "page.operations",
            dashboard: "page.dashboard",
            pricing: "page.pricing",
            products: "page.products",
            devices: "page.devices",
            billing: "page.billing",
            inventory: "page.inventory",
            reports: "page.reports",
            shifts: "page.shifts",
            users: "page.users",
            settings: "page.settings",
        };

        if (activePage === "pos") {
            return posSession
                ? t("pos.attachedTitle", { device: posSession.device?.name ?? "" })
                : t("page.pos");
        }

        if (titleKeys[activePage]) {
            return t(titleKeys[activePage]);
        }

        switch (activePage) {

            case "pricing":
                return "Pricing";

            case "products":
                return "Products";

            case "devices":
                return "Devices";

            case "billing":
                return "Billing";

            case "pos":
                return posSession
                    ? `POS — ${posSession.device?.name}`
                    : "Point of Sale";

            case "inventory":
                return "Inventory";

            case "reports":
                return "Reports";

            case "settings":
                return "Settings";

            case "operations":
                return "Operations";

            case "dashboard":
                return "Admin Dashboard";

            case "shifts":
                return "Cashier Shifts";

            case "users":
                return "Users & Roles";

            default:
                return "Operations";
        }
    }


    return (
        <Layout
            activePage={activePage}
            onNavigate={handleNavigate}
            title={getTitle()}
        >
            {renderPage()}
        </Layout>
    );
}
