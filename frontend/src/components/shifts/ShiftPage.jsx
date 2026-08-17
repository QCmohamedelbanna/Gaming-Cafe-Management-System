import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";
import { useAuth } from "../../auth/AuthContext";
import { closeShift, getAllShifts, getCurrentShift, getMyShifts, getShiftReport, openShift } from "../../api/shiftApi";

function translatedEnum(t, prefix, value) {
    if (!value) return t("common.dash");
    const key = prefix + "." + value;
    const translated = t(key);
    return translated === key ? value : translated;
}

export default function ShiftPage() {
    const { t, language, formatCurrency } = useLanguage();
    const { user, hasPermission, refreshCurrentShift } = useAuth();
    const canAuditAll = hasPermission("SHIFT_AUDIT");
    const [current, setCurrent] = useState(null);
    const [history, setHistory] = useState([]);
    const [allShifts, setAllShifts] = useState([]);
    const [report, setReport] = useState(null);
    const [openingCash, setOpeningCash] = useState("0");
    const [actualCash, setActualCash] = useState("");
    const [note, setNote] = useState("");
    const [loading, setLoading] = useState(true);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");

    const money = (value) => formatCurrency(value);
    const dateTime = (value) => value
        ? new Date(value).toLocaleString(language === "ar" ? "ar-EG" : "en-EG")
        : t("common.dash");
    const roleLabel = (role) => translatedEnum(t, "roles.role", role);

    async function load() {
        try {
            setLoading(true);
            setError("");
            const [currentShift, mine, everyone] = await Promise.all([
                getCurrentShift(),
                getMyShifts(),
                canAuditAll ? getAllShifts() : Promise.resolve([]),
            ]);
            setCurrent(currentShift);
            setHistory(mine || []);
            setAllShifts(everyone || []);
            if (currentShift) setActualCash(currentShift.expectedCash ?? "");
        } catch (loadError) {
            setError(loadError.message || t("shifts.loadError"));
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => { load(); }, [canAuditAll]);

    async function handleOpen(event) {
        event.preventDefault();
        setBusy(true);
        setError("");
        setMessage("");
        try {
            const shift = await openShift(openingCash);
            setCurrent(shift);
            await refreshCurrentShift();
            setMessage(t("shifts.opened"));
            await load();
        } catch (openError) {
            setError(openError.message || t("shifts.openError"));
        } finally {
            setBusy(false);
        }
    }

    async function handleClose(event) {
        event.preventDefault();
        setBusy(true);
        setError("");
        setMessage("");
        try {
            const closed = await closeShift(actualCash, note);
            setCurrent(null);
            await refreshCurrentShift();
            setMessage(t("shifts.closed", { amount: money(closed.cashDifference) }));
            setNote("");
            await load();
            await showReport(closed.id);
        } catch (closeError) {
            setError(closeError.message || t("shifts.closeError"));
        } finally {
            setBusy(false);
        }
    }

    async function showReport(id) {
        try {
            setError("");
            setReport(await getShiftReport(id));
        } catch (reportError) {
            setError(reportError.message || t("shifts.reportError"));
        }
    }

    const rows = canAuditAll ? allShifts : history;

    return (
        <div className="feature-page">
            <div className="feature-page-header">
                <div>
                    <span className="page-label">{t("shifts.pageLabel")}</span>
                    <h1>{t("shifts.title")}</h1>
                    <p>{t("shifts.description")}</p>
                </div>
            </div>

            {message && <div className="success-banner">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}

            {loading ? <p>{t("common.loading")}</p> : <>
                <div className="shift-summary-grid">
                    <div className="feature-card">
                        <span className="page-label">{t("shifts.currentUser")}</span>
                        <h2>{user?.displayName}</h2>
                        <p>{roleLabel(user?.role)} · @{user?.username}</p>
                    </div>
                    <div className="feature-card">
                        <span className="page-label">{t("shifts.shiftStatus")}</span>
                        <h2 className={current ? "status-positive" : "status-muted"}>
                            {current ? t("common.open") : t("common.closed")}
                        </h2>
                        <p>{current
                            ? t("shifts.started", { time: dateTime(current.openedAt) })
                            : t("shifts.noActive")}
                        </p>
                    </div>
                    {current && (
                        <div className="feature-card">
                            <span className="page-label">{t("shifts.expectedCash")}</span>
                            <h2>{money(current.expectedCash)}</h2>
                            <p>{t("shifts.openingFloat", { amount: money(current.openingCash) })}</p>
                        </div>
                    )}
                </div>

                {!current ? (
                    <section className="feature-card shift-action-card">
                        <div>
                            <span className="page-label">{t("shifts.startOfShift")}</span>
                            <h2>{t("shifts.openCashDrawer")}</h2>
                            <p>{t("shifts.openCashDescription")}</p>
                        </div>
                        <form className="inline-feature-form" onSubmit={handleOpen}>
                            <label>
                                {t("shifts.openingCash")}
                                <input type="number" min="0" step="0.01" value={openingCash} onChange={(event) => setOpeningCash(event.target.value)} required />
                            </label>
                            <button type="submit" className="product-add-button" disabled={busy}>
                                {busy ? t("shifts.opening") : t("shifts.openShift")}
                            </button>
                        </form>
                    </section>
                ) : (
                    <section className="feature-card shift-action-card">
                        <div>
                            <span className="page-label">{t("shifts.endOfShift")}</span>
                            <h2>{t("shifts.closeCashDrawer")}</h2>
                            <p>{t("shifts.closeCashDescription")}</p>
                        </div>
                        <form className="inline-feature-form" onSubmit={handleClose}>
                            <label>
                                {t("shifts.actualCash")}
                                <input type="number" min="0" step="0.01" value={actualCash} onChange={(event) => setActualCash(event.target.value)} required />
                            </label>
                            <label>
                                {t("shifts.note")}
                                <input value={note} onChange={(event) => setNote(event.target.value)} placeholder={t("shifts.notePlaceholder")} />
                            </label>
                            <button type="submit" className="danger-button" disabled={busy}>
                                {busy ? t("shifts.closing") : t("shifts.closeShift")}
                            </button>
                        </form>
                    </section>
                )}

                <section className="feature-card">
                    <div className="feature-card-heading">
                        <div>
                            <span className="page-label">{t("shifts.history")}</span>
                            <h2>{canAuditAll ? t("shifts.allCashierShifts") : t("shifts.myShifts")}</h2>
                        </div>
                        <span className="feature-count">{t("shifts.count", { count: rows.length })}</span>
                    </div>
                    <div className="feature-table-wrap">
                        <table className="feature-table">
                            <thead>
                                <tr>
                                    <th>{t("shifts.cashier")}</th><th>{t("shifts.openedAt")}</th><th>{t("shifts.closedAt")}</th>
                                    <th>{t("shifts.status")}</th><th>{t("shifts.expected")}</th><th>{t("shifts.difference")}</th><th />
                                </tr>
                            </thead>
                            <tbody>
                                {rows.map((shift) => (
                                    <tr key={shift.id}>
                                        <td><strong>{shift.displayName}</strong><small>@{shift.username}</small></td>
                                        <td>{dateTime(shift.openedAt)}</td>
                                        <td>{dateTime(shift.closedAt)}</td>
                                        <td><span className={shift.status === "OPEN" ? "status-positive" : "status-muted"}>{translatedEnum(t, "shifts.status", shift.status)}</span></td>
                                        <td>{money(shift.expectedCash)}</td>
                                        <td className={Number(shift.cashDifference || 0) < 0 ? "amount-negative" : "amount-positive"}>{shift.cashDifference == null ? t("common.dash") : money(shift.cashDifference)}</td>
                                        <td><button type="button" className="text-button" onClick={() => showReport(shift.id)}>{t("shifts.report")}</button></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                        {rows.length === 0 && <p className="admin-empty-state">{t("shifts.noRecords")}</p>}
                    </div>
                </section>

                {report && (
                    <section className="feature-card shift-report-card">
                        <div className="feature-card-heading">
                            <div>
                                <span className="page-label">{t("shifts.transactionReport")}</span>
                                <h2>#{report.shift.id} · {report.shift.displayName}</h2>
                            </div>
                            <button type="button" className="text-button" onClick={() => setReport(null)}>{t("shifts.closeReport")}</button>
                        </div>
                        <div className="shift-report-metrics">
                            <Metric label={t("shifts.cashSales")} value={money(report.cashCollected)} />
                            <Metric label={t("shifts.cardSales")} value={money(report.cardCollected)} />
                            <Metric label={t("shifts.walletSales")} value={money(report.mobileWalletCollected)} />
                            <Metric label={t("shifts.refunds")} value={money(report.refunds)} />
                            <Metric label={t("shifts.netCollected")} value={money(report.netCollected)} />
                        </div>
                        <div className="feature-table-wrap">
                            <table className="feature-table">
                                <thead>
                                    <tr>
                                        <th>{t("shifts.bill")}</th><th>{t("shifts.method")}</th><th>{t("shifts.amount")}</th>
                                        <th>{t("shifts.tendered")}</th><th>{t("shifts.change")}</th><th>{t("shifts.status")}</th><th>{t("shifts.time")}</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {(report.transactions || []).map((transaction) => (
                                        <tr key={transaction.paymentId}>
                                            <td>{transaction.billNumber || "#" + transaction.billId}</td>
                                            <td>{translatedEnum(t, "shifts.method", transaction.method)}</td>
                                            <td>{money(transaction.amount)}</td>
                                            <td>{money(transaction.amountTendered)}</td>
                                            <td>{money(transaction.changeAmount)}</td>
                                            <td>{translatedEnum(t, "shifts.paymentStatus", transaction.status)}</td>
                                            <td>{dateTime(transaction.paidAt)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </section>
                )}
            </>}
        </div>
    );
}

function Metric({ label, value }) {
    return <div className="shift-report-metric"><span>{label}</span><strong>{value}</strong></div>;
}
