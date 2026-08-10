import { useState } from "react";

import Layout from "./components/layout/Layout";
import Dashboard from "./components/dashboard/Dashboard";
import PricingPage from "./components/pricing/PricingPage";
import POSPage from "./components/pos/POSPage";

export default function App() {

    const [activePage, setActivePage] =
        useState("dashboard");

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

        /*
         * Clicking POS directly from sidebar
         * means standalone sale.
         */
        if (page === "pos") {
            setPosSession(null);
        }

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

            case "pricing":
                return <PricingPage />;

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
                    />
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

        switch (activePage) {

            case "pricing":
                return "Pricing";

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

            default:
                return "Dashboard";
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