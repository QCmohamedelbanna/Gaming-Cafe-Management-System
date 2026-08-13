import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";

import { getDevices } from "../../api/deviceApi";
import { getPricing } from "../../api/pricingApi";
import { getOpenOrderForSession } from "../../api/orderApi";
import { payBill, refundBill } from "../../api/billingApi";

import {
    getActiveSessions,
    startSession,
    prepareCheckout,
    extendSession,
    finishMatch,
    addMatch,
} from "../../api/sessionApi";

import { getProducts } from "../../api/productApi";

import {
    createOrder,
    addOrderItem,
} from "../../api/orderApi";

import DeviceCard from "./DeviceCard";
import StartSessionModal from "./StartSessionModal";
import CheckoutModal from "./CheckoutModal";
import ReceiptModal from "./ReceiptModal";


export default function Dashboard({ onAddOrder }) {
    const { t } = useLanguage();

    const [devices, setDevices] = useState([]);
    const [activeSessions, setActiveSessions] = useState([]);
    const [pricing, setPricing] = useState([]);

    const [loading, setLoading] = useState(true);
    const [starting, setStarting] = useState(false);

    const [selectedDevice, setSelectedDevice] = useState(null);

    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const [products, setProducts] = useState([]);

    const [addingProductId, setAddingProductId] = useState(null);
    const [sessionOrders, setSessionOrders] = useState({});
    const [checkout, setCheckout] = useState(null);
    const [preparingSessionId, setPreparingSessionId] = useState(null);
    const [checkingOut, setCheckingOut] = useState(false);
    const [receipt, setReceipt] = useState(null);
    const [refunding, setRefunding] = useState(false);


    async function loadSessionOrders(sessionsData) {
        try {
            const orders = {};

            await Promise.all(
                sessionsData.map(async (session) => {
                    try {
                        const order =
                            await getOpenOrderForSession(
                                session.id
                            );

                        orders[session.id] = order;
                    } catch (error) {
                        console.error(
                            `Could not load order for session ${session.id}`,
                            error
                        );

                        orders[session.id] = null;
                    }
                })
            );

            setSessionOrders(orders);

        } catch (error) {
            console.error(
                "Could not load session orders",
                error
            );
        }
    }


    async function handleQuickAddProduct(
        session,
        product
    ) {

        try {

            setAddingProductId(product.id);
            setMessage("");

            const order =
                await createOrder(session.id);

            await addOrderItem(
                order.id,
                product.id,
                1
            );

            await loadActiveSessions();
            setMessage(
                t("operations.addedProduct", {
                    product: product.name,
                    device: session.device?.name,
                })
            );

        } catch (error) {

            console.error(error);

            setMessage(
                error.message ||
                t("operations.addProductError")
            );

        } finally {

            setAddingProductId(null);
        }
    }
    // ================================
    // Load Dashboard
    // ================================

    async function loadDashboard() {

        try {

            setError("");

            const [
                devicesData,
                sessionsData,
                pricingData,
                productsData,
            ] = await Promise.all([
                getDevices(),
                getActiveSessions(),
                getPricing(),
                getProducts(),
            ]);

            setDevices(devicesData);
            setActiveSessions(sessionsData);
            setPricing(pricingData);
            setProducts(productsData);

            await loadSessionOrders(sessionsData);
        } catch (error) {

            console.error(
                "Dashboard loading error:",
                error
            );

            setError(
                error.message ||
                t("operations.loadError")
            );

        } finally {

            setLoading(false);
        }
    }




    // ================================
    // Refresh Active Sessions
    // ================================

    async function loadActiveSessions() {

        try {

            const sessions =
                await getActiveSessions();

            setActiveSessions(sessions);
            await loadSessionOrders(sessions);

        } catch (error) {

            console.error(
                "Active sessions error:",
                error
            );
        }
    }


    // ================================
    // Initial Load
    // ================================

    useEffect(() => {

        loadDashboard();

        const interval = setInterval(
            loadActiveSessions,
            5000
        );

        return () =>
            clearInterval(interval);

    }, []);


    // ================================
    // Find Session For Device
    // ================================

    function getDeviceSession(deviceId) {

        return activeSessions.find(
            (session) =>
                session.device?.id === deviceId
        );
    }


    // ================================
    // Start Session
    // ================================

    async function handleStartSession(data) {

        try {

            setStarting(true);
            setError("");
            setMessage("");

            await startSession(data);

            setSelectedDevice(null);

            await loadDashboard();

            setMessage(
                t("operations.sessionStarted")
            );

        } catch (error) {

            console.error(
                "Start session error:",
                error
            );

            setError(
                error.message ||
                t("operations.startSessionError")
            );

        } finally {

            setStarting(false);
        }
    }


    // ================================
    // Loading
    // ================================

    if (loading && devices.length === 0) {

        return (
            <p>{t("operations.loading")}</p>
        );
    }

    async function handleOpenCheckout(session, order) {
        if (preparingSessionId != null) return;

        try {
            setPreparingSessionId(session.id);
            setError("");
            setMessage("");

            const finalBill = await prepareCheckout(session.id);

            setCheckout({
                session,
                order,
                bill: finalBill,
            });

            await loadDashboard();
        } catch (error) {
            console.error("Checkout preparation error:", error);
            setError(
                error.message ||
                t("operations.stopSessionError")
            );
        } finally {
            setPreparingSessionId(null);
        }
    }

    async function handleCheckout(billId, payment) {
        try {
            setCheckingOut(true);
            setError("");

            const result = await payBill(billId, payment);

            setCheckout(null);
            setReceipt(result);
            await loadDashboard();

            setMessage(
                t("operations.billCompleted", {
                    bill: result.billNumber,
                    total: Number(result.totalAmount || 0).toFixed(2),
                })
            );

        } catch (error) {
            console.error(error);

            setError(
                error.message ||
                t("operations.checkoutError")
            );
        } finally {
            setCheckingOut(false);
        }
    }

    async function handleRefund(reason) {
        if (!receipt?.billId) return;

        try {
            setRefunding(true);
            const updated = await refundBill(receipt.billId, reason);
            setReceipt(updated);
            setMessage(t("operations.billRefunded", { bill: updated.billNumber }));
        } catch (error) {
            console.error(error);
            setError(error.message || t("billing.refundError"));
        } finally {
            setRefunding(false);
        }
    }

    async function handleExtendSession(sessionId, minutes) {
        try {
            setMessage("");

            await extendSession(sessionId, minutes);

            await loadDashboard();

        } catch (error) {
            console.error(error);

            setMessage(
                error.message ||
                t("operations.extendError")
            );
        }
    }

    async function handleFinishMatch(sessionId) {
        try {
            setMessage("");

            await finishMatch(sessionId);

            await loadDashboard();

        } catch (error) {
            console.error(error);

            setMessage(
                error.message ||
                t("operations.finishMatchError")
            );
        }
    }

    async function handleAddMatch(sessionId) {
        try {
            setMessage("");

            await addMatch(sessionId);

            await loadDashboard();

        } catch (error) {
            console.error(error);

            setMessage(
                error.message ||
                t("operations.addMatchError")
            );
        }
    }

    // ================================
    // UI
    // ================================

    return (

        <div className="dashboard-page">

            <div className="dashboard-toolbar">

                <div>

                    <span className="page-label">
                        {t("operations.live")}
                    </span>

                    <h2>
                        {t("operations.stations")}
                    </h2>

                    <p>
                        {t("operations.description")}
                    </p>

                </div>

                <button
                    className="refresh-button"
                    onClick={loadDashboard}
                >
                    {t("operations.refresh")}
                </button>

            </div>


            {error && (

                <div className="pricing-message">
                    {error}
                </div>

            )}


            {message && (

                <div className="pricing-message">
                    {message}
                </div>

            )}


            <div className="device-grid">

                {devices.map((device) => {

                    const session =
                        getDeviceSession(device.id);

                    const order =
                        session
                            ? sessionOrders[session.id] ?? null
                            : null;

                    return (
                        <DeviceCard
                            key={device.id}
                            device={device}

                            session={session}

                            order={order}
                            onStart={setSelectedDevice}
                            onStop={handleOpenCheckout}
                            checkoutLoading={
                                preparingSessionId === session?.id
                            }
                            onExtend={handleExtendSession}
                            onFinishMatch={handleFinishMatch}
                            onAddMatch={handleAddMatch}

                            products={products}
                            addingProductId={addingProductId}
                            onQuickAddProduct={handleQuickAddProduct}
                        />
                    );
                })}

            </div>


            {selectedDevice && (

                <StartSessionModal
                    device={selectedDevice}
                    pricing={pricing}
                    loading={starting}
                    onClose={() =>
                        setSelectedDevice(null)
                    }
                    onStart={
                        handleStartSession
                    }
                />

            )}

            {checkout && (
                <CheckoutModal
                    device={checkout.session.device}
                    session={checkout.session}
                    order={checkout.order}
                    finalBill={checkout.bill}
                    loading={checkingOut}
                    error={error}
                    onClose={() => setCheckout(null)}
                    onCheckout={handleCheckout}
                />
            )}

            {receipt && (
                <ReceiptModal
                    bill={receipt}
                    refunding={refunding}
                    onClose={() => setReceipt(null)}
                    onRefund={handleRefund}
                />
            )}

        </div>

    );
}
