import { useCallback, useEffect, useState } from "react";
import { useLanguage } from "../../i18n";

import { getDashboardSummary } from "../../api/dashboardApi";

export default function AdminDashboardPage() {
    const { t, formatCurrency, formatNumber, language } = useLanguage();
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState("");

    const loadSummary = useCallback(async (silent = false) => {
        try {
            if (silent) setRefreshing(true);
            else setLoading(true);
            setError("");
            setSummary(await getDashboardSummary());
        } catch (loadError) {
            console.error(loadError);
            setError(loadError.message || t("admin.loadError"));
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, [t]);

    useEffect(() => {
        loadSummary();
        const intervalId = window.setInterval(() => loadSummary(true), 30000);
        return () => window.clearInterval(intervalId);
    }, [loadSummary]);

    const paymentMethods = summary?.salesByPaymentMethod || [];
    const endingSoon = summary?.sessionsEndingSoon || [];
    const maxPaymentAmount = Math.max(
        1,
        ...paymentMethods.map((payment) => Number(payment.amount || 0))
    );

    if (loading && !summary) {
        return <div className="admin-dashboard-page"><p>{t("admin.loading")}</p></div>;
    }

    const formatMoney = (value) => formatCurrency(value);
    const paymentLabel = (value) => {
        const key = String(value || "").toUpperCase();
        if (key === "CASH") return t("common.cash");
        if (key === "CARD") return t("common.card");
        if (key === "MOBILE_WALLET") return t("common.mobileWallet");
        return value || t("admin.unknown");
    };
    const sessionLabel = (value) => {
        const key = String(value || "").toUpperCase();
        if (key === "SINGLE") return t("modal.single");
        if (key === "MULTI") return t("modal.multi");
        if (key === "MATCH") return t("modal.match");
        return value || t("admin.session");
    };
    const formatTimeLocalized = (value) => value
        ? new Date(value).toLocaleTimeString(language === "ar" ? "ar-EG" : "en-EG", { hour: "2-digit", minute: "2-digit" })
        : "—";
    const formatRemainingLocalized = (seconds) => {
        const totalSeconds = Math.max(0, Number(seconds || 0));
        const minutes = Math.floor(totalSeconds / 60);
        const remainder = totalSeconds % 60;
        return `${formatNumber(minutes)}${t("admin.minutes")} ${String(remainder).padStart(2, "0")}${t("admin.seconds")}`;
    };

    return (
        <div className="admin-dashboard-page">
            <div className="admin-dashboard-header">
                <div>
                    <span className="page-label">{t("admin.today")}</span>
                    <h1>{t("admin.title")}</h1>
                    <p>{t("admin.description")}</p>
                </div>
                <button
                    type="button"
                    className="product-secondary-button"
                    onClick={() => loadSummary(true)}
                    disabled={refreshing}
                >
                    {refreshing ? t("admin.refreshing") : t("common.refresh")}
                </button>
            </div>

            {error && <div className="product-error-message">{error}</div>}

            <div className="admin-metric-grid">
                <MetricCard label={t("admin.activeSessions")} value={formatNumber(summary?.activeSessions ?? 0)} tone="blue" />
                <MetricCard label={t("admin.availableDevices")} value={formatNumber(summary?.availableDevices ?? 0)} detail={t("admin.offline", { count: formatNumber(summary?.offlineDevices ?? 0) })} tone="green" />
                <MetricCard label={t("admin.gamingRevenue")} value={formatMoney(summary?.gamingRevenueToday)} tone="purple" />
                <MetricCard label={t("admin.cafeRevenue")} value={formatMoney(summary?.cafeRevenueToday ?? summary?.productsRevenueToday)} tone="orange" />
                <MetricCard label={t("admin.totalRevenue")} value={formatMoney(summary?.totalRevenueToday ?? summary?.revenueToday)} tone="green" />
                <MetricCard label={t("admin.completedBills")} value={formatNumber(summary?.completedBillsToday ?? summary?.paidBillsToday ?? 0)} tone="blue" />
                <MetricCard label={t("admin.averageBill")} value={formatMoney(summary?.averageBillValueToday)} tone="purple" />
                <MetricCard label={t("admin.lowStock")} value={formatNumber(summary?.lowStockProducts ?? 0)} detail={t("admin.needsAttention")} tone={(summary?.lowStockProducts ?? 0) > 0 ? "warning" : "green"} />
            </div>

            <div className="admin-dashboard-grid">
                <section className="admin-panel">
                    <div className="admin-panel-heading">
                        <div>
                            <span className="page-label">{t("admin.liveOperations")}</span>
                            <h2>{t("admin.sessionsEndingSoon")}</h2>
                        </div>
                        <span className="admin-panel-count">{endingSoon.length}</span>
                    </div>
                    {endingSoon.length === 0 ? (
                        <p className="admin-empty-state">{t("admin.noEndingSoon")}</p>
                    ) : (
                        <div className="admin-alert-list">
                            {endingSoon.map((session) => (
                                <div className="admin-alert-row" key={session.sessionId}>
                                    <div>
                                        <strong>{session.deviceName}</strong>
                                        <span>{t("admin.endsAt", { type: sessionLabel(session.sessionType), time: formatTimeLocalized(session.endsAt) })}</span>
                                    </div>
                                    <b>{formatRemainingLocalized(session.remainingSeconds)}</b>
                                </div>
                            ))}
                        </div>
                    )}
                </section>

                <section className="admin-panel">
                    <div className="admin-panel-heading">
                        <div>
                            <span className="page-label">{t("admin.completedBillsLabel")}</span>
                            <h2>{t("admin.paymentMethods")}</h2>
                        </div>
                    </div>
                    {paymentMethods.length === 0 ? (
                        <p className="admin-empty-state">{t("admin.noSales")}</p>
                    ) : (
                        <div className="admin-payment-list">
                            {paymentMethods.map((payment) => {
                                const amount = Number(payment.amount || 0);
                                return (
                                    <div className="admin-payment-row" key={payment.method}>
                                        <div className="admin-payment-label">
                                            <strong>{paymentLabel(payment.method)}</strong>
                                            <span>{t("admin.transactions", { count: formatNumber(payment.transactionCount || 0), suffix: language === "ar" ? "" : payment.transactionCount === 1 ? "" : "s" })}</span>
                                        </div>
                                        <div className="admin-payment-bar-track">
                                            <span style={{ width: `${Math.max(0, Math.min(100, amount / maxPaymentAmount * 100))}%` }} />
                                        </div>
                                        <b>{formatMoney(amount)}</b>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </section>
            </div>
        </div>
    );
}

function MetricCard({ label, value, detail, tone }) {
    return (
        <div className={`admin-metric-card ${tone || ""}`}>
            <span>{label}</span>
            <strong>{value}</strong>
            {detail && <small>{detail}</small>}
        </div>
    );
}
