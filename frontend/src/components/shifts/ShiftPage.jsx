import { useEffect, useState } from "react";
import { useAuth } from "../../auth/AuthContext";
import { closeShift, getAllShifts, getCurrentShift, getMyShifts, getShiftReport, openShift } from "../../api/shiftApi";

function money(value) {
    return `EGP ${Number(value || 0).toFixed(2)}`;
}

function dateTime(value) {
    return value ? new Date(value).toLocaleString() : "—";
}

export default function ShiftPage() {
    const { user, hasRole } = useAuth();
    const canAuditAll = hasRole("MANAGER", "ADMIN");
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
            setError(loadError.message || "Could not load shifts");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => { load(); }, [canAuditAll]);

    async function handleOpen(event) {
        event.preventDefault();
        setBusy(true); setError(""); setMessage("");
        try {
            const shift = await openShift(openingCash);
            setCurrent(shift);
            setMessage("Shift opened. Payments will now be assigned to this shift.");
            await load();
        } catch (openError) {
            setError(openError.message || "Could not open shift");
        } finally { setBusy(false); }
    }

    async function handleClose(event) {
        event.preventDefault();
        setBusy(true); setError(""); setMessage("");
        try {
            const closed = await closeShift(actualCash, note);
            setCurrent(null);
            setMessage(`Shift closed. Cash difference: ${money(closed.cashDifference)}.`);
            setNote("");
            await load();
            await showReport(closed.id);
        } catch (closeError) {
            setError(closeError.message || "Could not close shift");
        } finally { setBusy(false); }
    }

    async function showReport(id) {
        try {
            setError("");
            setReport(await getShiftReport(id));
        } catch (reportError) {
            setError(reportError.message || "Could not load shift report");
        }
    }

    const rows = canAuditAll ? allShifts : history;

    return (
        <div className="feature-page">
            <div className="feature-page-header"><div><span className="page-label">CASH CONTROL</span><h1>Cashier shifts</h1><p>Open a till, reconcile expected cash, and review shift transactions.</p></div><button type="button" className="refresh-button" onClick={load}>Refresh</button></div>
            {message && <div className="success-banner">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}
            {loading ? <p>Loading shifts…</p> : <>
                <div className="shift-summary-grid">
                    <div className="feature-card"><span className="page-label">CURRENT USER</span><h2>{user.displayName}</h2><p>{user.role} · @{user.username}</p></div>
                    <div className="feature-card"><span className="page-label">SHIFT STATUS</span><h2 className={current ? "status-positive" : "status-muted"}>{current ? "OPEN" : "CLOSED"}</h2><p>{current ? `Started ${dateTime(current.openedAt)}` : "No active shift"}</p></div>
                    {current && <div className="feature-card"><span className="page-label">EXPECTED CASH</span><h2>{money(current.expectedCash)}</h2><p>Opening float: {money(current.openingCash)}</p></div>}
                </div>

                {!current ? (
                    <section className="feature-card shift-action-card"><div><span className="page-label">START OF SHIFT</span><h2>Open cash drawer</h2><p>Enter the cash float counted before taking payments.</p></div><form className="inline-feature-form" onSubmit={handleOpen}><label>Opening cash<input type="number" min="0" step="0.01" value={openingCash} onChange={(event) => setOpeningCash(event.target.value)} required /></label><button type="submit" className="product-add-button" disabled={busy}>{busy ? "Opening…" : "Open shift"}</button></form></section>
                ) : (
                    <section className="feature-card shift-action-card"><div><span className="page-label">END OF SHIFT</span><h2>Close cash drawer</h2><p>Expected cash is calculated from the opening float, cash payments, and cash refunds.</p></div><form className="inline-feature-form" onSubmit={handleClose}><label>Actual cash counted<input type="number" min="0" step="0.01" value={actualCash} onChange={(event) => setActualCash(event.target.value)} required /></label><label>Note<input value={note} onChange={(event) => setNote(event.target.value)} placeholder="Optional reconciliation note" /></label><button type="submit" className="danger-button" disabled={busy}>{busy ? "Closing…" : "Close shift"}</button></form></section>
                )}

                <section className="feature-card"><div className="feature-card-heading"><div><span className="page-label">SHIFT HISTORY</span><h2>{canAuditAll ? "All cashier shifts" : "My shifts"}</h2></div><span className="feature-count">{rows.length} shifts</span></div><div className="feature-table-wrap"><table className="feature-table"><thead><tr><th>Cashier</th><th>Opened</th><th>Closed</th><th>Status</th><th>Expected</th><th>Difference</th><th /></tr></thead><tbody>{rows.map((shift) => <tr key={shift.id}><td><strong>{shift.displayName}</strong><small>@{shift.username}</small></td><td>{dateTime(shift.openedAt)}</td><td>{dateTime(shift.closedAt)}</td><td><span className={shift.status === "OPEN" ? "status-positive" : "status-muted"}>{shift.status}</span></td><td>{money(shift.expectedCash)}</td><td className={Number(shift.cashDifference || 0) < 0 ? "amount-negative" : "amount-positive"}>{shift.cashDifference == null ? "—" : money(shift.cashDifference)}</td><td><button type="button" className="text-button" onClick={() => showReport(shift.id)}>Report</button></td></tr>)}</tbody></table>{rows.length === 0 && <p className="admin-empty-state">No shifts recorded yet.</p>}</div></section>

                {report && <section className="feature-card shift-report-card"><div className="feature-card-heading"><div><span className="page-label">TRANSACTION REPORT</span><h2>Shift #{report.shift.id} · {report.shift.displayName}</h2></div><button type="button" className="text-button" onClick={() => setReport(null)}>Close report</button></div><div className="shift-report-metrics"><Metric label="Cash sales" value={money(report.cashCollected)} /><Metric label="Card sales" value={money(report.cardCollected)} /><Metric label="Wallet sales" value={money(report.mobileWalletCollected)} /><Metric label="Refunds" value={money(report.refunds)} /><Metric label="Net collected" value={money(report.netCollected)} /></div><div className="feature-table-wrap"><table className="feature-table"><thead><tr><th>Bill</th><th>Method</th><th>Amount</th><th>Tendered</th><th>Change</th><th>Status</th><th>Time</th></tr></thead><tbody>{(report.transactions || []).map((transaction) => <tr key={transaction.paymentId}><td>{transaction.billNumber || `#${transaction.billId}`}</td><td>{transaction.method}</td><td>{money(transaction.amount)}</td><td>{money(transaction.amountTendered)}</td><td>{money(transaction.changeAmount)}</td><td>{transaction.status}</td><td>{dateTime(transaction.paidAt)}</td></tr>)}</tbody></table></div></section>}
            </>}
        </div>
    );
}

function Metric({ label, value }) { return <div className="shift-report-metric"><span>{label}</span><strong>{value}</strong></div>; }
