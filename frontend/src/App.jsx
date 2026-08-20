import { useState } from "react";
import { useLanguage } from "./i18n";
import { useAuth } from "./auth/AuthContext";

import Layout from "./components/layout/Layout";
import LoginPage from "./components/auth/LoginPage";
import Dashboard from "./components/dashboard/Dashboard";
import ReservationsPage from "./components/reservations/ReservationsPage";
import SettingsPage from "./components/settings/SettingsPage";
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
import PermissionsPage from "./components/permissions/PermissionsPage";
import RolesPage from "./components/roles/RolesPage.jsx";

export default function App() {
    const { t } = useLanguage();
    const { user, loading, hasPermission } = useAuth();

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
        const permissionByPage = {
            operations: "OPERATIONS_USE",
            reservations: "RESERVATIONS_MANAGE",
            pos: "POS_USE",
            billing: "CHECKOUT_USE",
            shifts: "SHIFT_MANAGE",
            dashboard: "DASHBOARD_VIEW",
            products: "PRODUCTS_MANAGE",
            pricing: "PRICING_MANAGE",
            inventory: "INVENTORY_MANAGE",
            reports: "REPORTS_VIEW",
            devices: "DEVICES_MANAGE",
            users: "USERS_MANAGE",
            permissions: "PERMISSIONS_MANAGE",
            rules: "PERMISSIONS_MANAGE",
            settings: "SETTINGS_MANAGE",
        };
        const requiredPermission = permissionByPage[page];
        return Boolean(requiredPermission && hasPermission(requiredPermission));
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

            case "reservations":
                return <ReservationsPage />;

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

            case "permissions":
                return <PermissionsPage />;

            case "rules":
                return <RolesPage />;

            case "settings":
                return <SettingsPage />;

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
            reservations: "page.reservations",
            dashboard: "page.dashboard",
            pricing: "page.pricing",
            products: "page.products",
            devices: "page.devices",
            billing: "page.billing",
            inventory: "page.inventory",
            reports: "page.reports",
            shifts: "page.shifts",
            users: "page.users",
            permissions: "page.permissions",
            rules: "page.rules",
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
                return "Users";

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
