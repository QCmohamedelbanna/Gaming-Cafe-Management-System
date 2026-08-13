import { useState } from "react";
import { useLanguage } from "./i18n";

import Layout from "./components/layout/Layout";
import Dashboard from "./components/dashboard/Dashboard";
import AdminDashboardPage from "./components/dashboard/AdminDashboardPage";
import PricingPage from "./components/pricing/PricingPage";
import POSPage from "./components/pos/POSPage";
import ProductsPage from "./components/products/ProductsPage";
import BillingPage from "./components/billing/BillingPage";
import DevicesPage from "./components/devices/DevicesPage";
import InventoryPage from "./components/inventory/InventoryPage";
import ReportsPage from "./components/reports/ReportsPage";

export default function App() {
    const { t } = useLanguage();

    const [activePage, setActivePage] =
        useState("operations");

    /*
     * Session currently attached to POS.
     *
     * null = standalone sale
     */
    const [posSession, setPosSession] =
        useState(null);


    /*
     * Normal sidebar navigation
     */
    function handleNavigate(page) {

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
