import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";

import { getReport } from "../../api/reportApi";

const periodOptions = [
    ["daily", "reports.today"],
    ["weekly", "reports.thisWeek"],
    ["monthly", "reports.thisMonth"],
    ["custom", "reports.customRange"],
];

export default function ReportsPage() {
    const { t, formatCurrency, formatNumber, language } = useLanguage();
    const initialRange = getRange("daily");
    const [period, setPeriod] = useState("daily");
    const [from, setFrom] = useState(initialRange.from);
    const [to, setTo] = useState(initialRange.to);
    const [report, setReport] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        loadReport(initialRange.from, initialRange.to);
        // The initial date range should be fetched only once on mount.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    async function loadReport(start, end) {
        try {
            setLoading(true);
            setError("");
            setReport(await getReport(start, end));
        } catch (loadError) {
            console.error(loadError);
            setError(loadError.message || t("reports.loadError"));
        } finally {
            setLoading(false);
        }
    }

    function selectPeriod(nextPeriod) {
        setPeriod(nextPeriod);
        if (nextPeriod === "custom") return;
        const nextRange = getRange(nextPeriod);
        setFrom(nextRange.from);
        setTo(nextRange.to);
        loadReport(nextRange.from, nextRange.to);
    }

    function applyCustomRange() {
        if (!from || !to) {
            setError(t("reports.chooseDates"));
            return;
        }
        if (to < from) {
            setError(t("reports.invalidDates"));
            return;
        }
        loadReport(from, to);
    }

    function exportCsv() {
        if (!report) return;
        const csv = buildCsv(report, t);
        const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = `gaming-cafe-report-${report.from}-${report.to}.csv`;
        link.click();
        URL.revokeObjectURL(url);
    }

    function exportPdf() {
        const previousTitle = document.title;
        document.title = `Gaming Cafe Report ${from} to ${to}`;
        window.print();
        window.setTimeout(() => {
            document.title = previousTitle;
        }, 500);
    }

    return (
        <div className="reports-page">
            <div className="reports-header">
                <div>
                    <span className="page-label">{t("reports.pageLabel")}</span>
                    <h1>{t("page.reports")}</h1>
                    <p>{t("reports.descriptionShort")}</p>
                </div>
                <div className="reports-export-actions no-print">
                    <button type="button" className="product-secondary-button" onClick={exportCsv} disabled={!report}>{t("reports.exportCsv")}</button>
                    <button type="button" className="product-add-button" onClick={exportPdf} disabled={!report}>{t("reports.printPdf")}</button>
                </div>
            </div>

            <section className="reports-filter-panel no-print">
                <div className="reports-period-buttons">
                    {periodOptions.map(([value, label]) => (
                        <button
                            type="button"
                            key={value}
                            className={period === value ? "active" : ""}
                            onClick={() => selectPeriod(value)}
                        >
                            {t(label)}
                        </button>
                    ))}
                </div>
                {period === "custom" && (
                    <div className="reports-custom-range">
                        <label>{t("reports.from")}<input type="date" value={from} onChange={(event) => setFrom(event.target.value)} /></label>
                        <label>{t("reports.to")}<input type="date" value={to} onChange={(event) => setTo(event.target.value)} /></label>
                        <button type="button" className="product-add-button" onClick={applyCustomRange}>{t("reports.apply")}</button>
                    </div>
                )}
                <span className="reports-range-label">{t("reports.rangeShowing", { from, to })}</span>
            </section>

            {error && <div className="product-error-message">{error}</div>}
            {loading ? (
                <p>{t("reports.loading")}</p>
            ) : report ? (
                <div className="report-print-area">
                    <div className="report-summary-grid">
                        <ReportMetric label={t("reports.gamingRevenue")} value={formatCurrency(report.gamingRevenue)} />
                        <ReportMetric label={t("reports.cafeRevenue")} value={formatCurrency(report.cafeRevenue)} />
                        <ReportMetric label={t("reports.totalRevenue")} value={formatCurrency(report.totalRevenue)} tone="highlight" />
                        <ReportMetric label={t("reports.completedBills")} value={formatNumber(report.completedBills)} />
                        <ReportMetric label={t("reports.averageBill")} value={formatCurrency(report.averageBillValue)} />
                        <ReportMetric label={t("reports.discounts")} value={formatCurrency(report.discountAmount)} tone="warning" />
                        <ReportMetric label={t("reports.cancellations")} value={`${formatNumber(report.cancelledBills)} · ${formatCurrency(report.cancelledAmount)}`} tone="warning" />
                        <ReportMetric label={t("reports.refunds")} value={`${formatNumber(report.refundedBills)} · ${formatCurrency(report.refundedAmount)}`} tone="danger" />
                    </div>

                    <div className="reports-section-grid">
                        <ReportTable title={t("reports.paymentTotals")} columns={[t("reports.method"), t("reports.transactions"), t("reports.amount")]} rows={(report.paymentMethods || []).map((item) => [formatLabel(item.method, t), formatNumber(item.transactionCount), formatCurrency(item.amount)])} emptyLabel={t("reports.noDataRange")} rowsLabel={t("reports.rows")} rowSuffix={language === "ar" ? "" : "s"} />
                        <ReportTable title={t("reports.revenueBySession")} columns={[t("reports.sessionType"), t("reports.bills"), t("reports.gaming"), t("reports.cafe"), t("reports.total")]} rows={(report.revenueBySessionType || []).map((item) => toRevenueRow(item, formatCurrency, formatNumber, t))} emptyLabel={t("reports.noDataRange")} rowsLabel={t("reports.rows")} rowSuffix={language === "ar" ? "" : "s"} />
                    </div>

                    <ReportTable
                        title={t("reports.revenueByDevice")}
                        columns={[t("reports.deviceSource"), t("reports.bills"), t("reports.gaming"), t("reports.cafe"), t("reports.total")]}
                        rows={(report.revenueByDevice || []).map((item) => toRevenueRow(item, formatCurrency, formatNumber, t))}
                        emptyLabel={t("reports.noDataRange")} rowsLabel={t("reports.rows")} rowSuffix={language === "ar" ? "" : "s"}
                    />

                    <ReportTable
                        title={t("reports.productProfitability")}
                        columns={[t("reports.product"), t("reports.sku"), t("reports.quantity"), t("reports.grossSales"), t("reports.discount"), t("reports.netSales"), t("reports.cost"), t("reports.profit")]}
                        rows={(report.productSales || []).map((item) => [
                            item.productName,
                            item.sku || "—",
                            `${formatNumber(item.quantity)} ${item.unit || ""}`.trim(),
                            formatCurrency(item.grossSales),
                            formatCurrency(item.discountAmount),
                            formatCurrency(item.netSales),
                            formatCurrency(item.costAmount),
                            formatCurrency(item.profit),
                        ])}
                        emptyLabel={t("reports.noDataRange")} rowsLabel={t("reports.rows")} rowSuffix={language === "ar" ? "" : "s"}
                    />

                    <ReportTable
                        title={t("reports.cashierReconciliation")}
                        columns={[t("reports.cashier"), t("reports.payments"), t("reports.collected"), t("reports.cash"), t("reports.card"), t("reports.mobileWallet"), t("reports.refundsLabel"), t("reports.net")]}
                        rows={(report.cashierShifts || []).map((item) => [
                            item.cashier,
                            item.paymentCount,
                            formatCurrency(item.totalCollected),
                            formatCurrency(item.cashCollected),
                            formatCurrency(item.cardCollected),
                            formatCurrency(item.mobileWalletCollected),
                            formatCurrency(item.refunds),
                            formatCurrency(item.netCollected),
                        ])}
                        emptyLabel={t("reports.noDataRange")} rowsLabel={t("reports.rows")} rowSuffix={language === "ar" ? "" : "s"}
                    />
                </div>
            ) : (
                <div className="reports-empty-state">{t("reports.noReport")}</div>
            )}
        </div>
    );
}

function ReportMetric({ label, value, tone }) {
    return <div className={`report-metric ${tone || ""}`}><span>{label}</span><strong>{value}</strong></div>;
}

function ReportTable({ title, columns, rows, emptyLabel = "No data for this range.", rowsLabel = "{{count}} rows", rowSuffix = "s" }) {
    return (
        <section className="report-table-section">
            <div className="report-table-heading"><h2>{title}</h2><span>{rowsLabel.replace("{{count}}", String(rows.length)).replace("{{suffix}}", rows.length === 1 ? "" : rowSuffix)}</span></div>
            {rows.length === 0 ? (
                <p className="admin-empty-state">{emptyLabel}</p>
            ) : (
                <div className="report-table-wrap">
                    <table className="report-table">
                        <thead><tr>{columns.map((column) => <th key={column}>{column}</th>)}</tr></thead>
                        <tbody>{rows.map((row, rowIndex) => <tr key={`${title}-${rowIndex}`}>{row.map((cell, cellIndex) => <td key={`${rowIndex}-${cellIndex}`}>{cell}</td>)}</tr>)}</tbody>
                    </table>
                </div>
            )}
        </section>
    );
}

function getRange(period) {
    const today = new Date();
    const todayValue = formatDate(today);
    if (period === "daily") return { from: todayValue, to: todayValue };
    if (period === "monthly") {
        return { from: formatDate(new Date(today.getFullYear(), today.getMonth(), 1)), to: todayValue };
    }
    const dayFromMonday = (today.getDay() + 6) % 7;
    const monday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - dayFromMonday);
    return { from: formatDate(monday), to: todayValue };
}

function formatDate(value) {
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, "0");
    const day = String(value.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function formatLabel(value, t) {
    const key = String(value || "").toUpperCase();
    if (key === "CASH") return t("reports.cash");
    if (key === "CARD") return t("reports.card");
    if (key === "MOBILE_WALLET") return t("reports.mobileWallet");
    return value || t("reports.unknown");
}

function toRevenueRow(item, formatCurrency, formatNumber, t) {
    const label = String(item.label || "").toUpperCase();
    const localizedLabel = label === "SINGLE" ? t("modal.single") : label === "MULTI" ? t("modal.multi") : label === "MATCH" ? t("modal.match") : item.label;
    return [localizedLabel, formatNumber(item.billCount), formatCurrency(item.gamingRevenue), formatCurrency(item.cafeRevenue), formatCurrency(item.totalRevenue)];
}

function buildCsv(report, t) {
    const rows = [["Section", "Name", "Value 1", "Value 2", "Value 3", "Value 4", "Value 5", "Value 6", "Value 7"]];
    rows.push([t("reports.total"), t("reports.gamingRevenue"), report.gamingRevenue]);
    rows.push([t("reports.total"), t("reports.cafeRevenue"), report.cafeRevenue]);
    rows.push([t("reports.total"), t("reports.totalRevenue"), report.totalRevenue]);
    rows.push([t("reports.total"), t("reports.discounts"), report.discountAmount]);
    rows.push([t("reports.total"), t("reports.completedBills"), report.completedBills]);
    rows.push([t("reports.total"), t("reports.cancellations"), report.cancelledBills, report.cancelledAmount]);
    rows.push([t("reports.total"), t("reports.refunds"), report.refundedBills, report.refundedAmount]);
    (report.paymentMethods || []).forEach((item) => rows.push([t("reports.paymentTotals"), item.method, item.transactionCount, item.amount]));
    (report.revenueByDevice || []).forEach((item) => rows.push([t("reports.revenueByDevice"), item.label, item.billCount, item.gamingRevenue, item.cafeRevenue, item.totalRevenue]));
    (report.revenueBySessionType || []).forEach((item) => rows.push([t("reports.revenueBySession"), item.label, item.billCount, item.gamingRevenue, item.cafeRevenue, item.totalRevenue]));
    (report.productSales || []).forEach((item) => rows.push([t("reports.product"), item.productName, item.sku, item.quantity, item.grossSales, item.discountAmount, item.netSales, item.costAmount, item.profit]));
    (report.cashierShifts || []).forEach((item) => rows.push([t("reports.cashier"), item.cashier, item.paymentCount, item.totalCollected, item.cashCollected, item.cardCollected, item.mobileWalletCollected, item.refunds, item.netCollected]));
    return rows.map((row) => row.map(csvCell).join(",")).join("\n");
}

function csvCell(value) {
    const text = value == null ? "" : String(value);
    return /[",\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
}
