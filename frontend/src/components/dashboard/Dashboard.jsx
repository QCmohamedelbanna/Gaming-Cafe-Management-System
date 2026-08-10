import { useEffect, useState } from "react";

import { getDevices } from "../../api/deviceApi";
import { getPricing } from "../../api/pricingApi";


import {
    getActiveSessions,
    startSession,
    stopSession,
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


export default function Dashboard({ onAddOrder }) {

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

            setMessage(
                `${product.name} added to ${session.device?.name}`
            );

        } catch (error) {

            console.error(error);

            setMessage(
                error.message ||
                "Could not add product."
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

        } catch (error) {

            console.error(
                "Dashboard loading error:",
                error
            );

            setError(
                error.message ||
                "Failed to load dashboard."
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
                "Session started successfully."
            );

        } catch (error) {

            console.error(
                "Start session error:",
                error
            );

            setError(
                error.message ||
                "Could not start session."
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
            <p>Loading dashboard...</p>
        );
    }

    async function handleStopSession(sessionId) {
        try {
            setMessage("");

            await stopSession(sessionId);

            await loadDashboard();

        } catch (error) {
            console.error(error);

            setMessage(
                error.message ||
                "Could not stop session."
            );
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
                "Could not extend session."
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
                "Could not finish match."
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
                "Could not add match."
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
                        LIVE OPERATIONS
                    </span>

                    <h2>
                        Gaming Stations
                    </h2>

                    <p>
                        Manage active PlayStation sessions.
                    </p>

                </div>

                <button
                    className="refresh-button"
                    onClick={loadDashboard}
                >
                    Refresh
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

                {devices.map((device) => (

                    <DeviceCard
                        key={device.id}
                        device={device}
                        session={getDeviceSession(device.id)}

                        onStart={setSelectedDevice}

                        onStop={handleStopSession}

                        onExtend={handleExtendSession}

                        onFinishMatch={handleFinishMatch}

                        onAddMatch={handleAddMatch}

                        products={products}

                        addingProductId={addingProductId}

                        onQuickAddProduct={handleQuickAddProduct}
                    />

                ))}

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

        </div>

    );
}