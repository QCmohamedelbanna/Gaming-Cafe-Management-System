import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";
import { useAuth } from "../../auth/AuthContext";
import {
    cancelBill,
    getPendingBills,
    payBill,
    refundBill,
} from "../../api/billingApi";
import BillPaymentModal from "./BillPaymentModal";
import CancelBillModal from "./CancelBillModal";
import ReceiptModal from "../dashboard/ReceiptModal";

export default function BillingPage() {
    const { t, formatCurrency } = useLanguage();
    const { hasPermission } = useAuth();
    const canRefund = hasPermission("BILL_REFUND");
    const [bills, setBills] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedBill, setSelectedBill] = useState(null);
    const [cancelTarget, setCancelTarget] = useState(null);
    const [processing, setProcessing] = useState(false);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");
    const [receipt, setReceipt] = useState(null);
    const [refunding, setRefunding] = useState(false);

    async function loadBills() {
        try {
            setLoading(true);
            setError("");
            setBills(await getPendingBills());
        } catch (loadError) {
            console.error(loadError);
            setError(loadError.message || t("billing.loadError"));
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadBills();
    }, []);

    async function handlePay(payment) {
        try {
            setProcessing(true);
            setError("");
            const paid = await payBill(selectedBill.billId, payment);
            setBills((current) => current.filter((bill) => bill.billId !== paid.billId));
            setSelectedBill(null);
            setReceipt(paid);
            setMessage(t("billing.paid", { bill: paid.billNumber }));
        } catch (payError) {
            console.error(payError);
            setError(payError.message || t("billing.payError"));
        } finally {
            setProcessing(false);
        }
    }

    async function handleCancel(bill) {
        try {
            setProcessing(true);
            setError("");
            await cancelBill(bill.billId);
            setBills((current) => current.filter((item) => item.billId !== bill.billId));
            setCancelTarget(null);
            setMessage(t("billing.cancelled", { bill: bill.billNumber }));
        } catch (cancelError) {
            console.error(cancelError);
            setError(cancelError.message || t("billing.cancelError"));
        } finally {
            setProcessing(false);
        }
    }

    async function handleRefund(reason) {
        if (!receipt?.billId) return;

        try {
            setRefunding(true);
            setReceipt(await refundBill(receipt.billId, reason));
        } catch (refundError) {
            console.error(refundError);
            setError(refundError.message || t("billing.refundError"));
        } finally {
            setRefunding(false);
        }
    }

    return (
        <div className="billing-page">
            <div className="billing-page-header">
                <div>
                    <span className="page-label">{t("billing.pageLabel")}</span>
                    <h1>{t("billing.title")}</h1>
                    <p>{t("billing.descriptionShort")}</p>
                </div>
            </div>

            {message && <div className="pricing-message">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}

            <section className="pending-bills-section">
                <div className="pending-bills-heading">
                    <div>
                        <span className="page-label">{t("billing.actionLabel")}</span>
                        <h2>{t("billing.pendingBills")}</h2>
                    </div>
                    <span className="pending-bills-count">{bills.length}</span>
                </div>

                {loading ? (
                    <p>{t("billing.loading")}</p>
                ) : bills.length === 0 ? (
                    <div className="products-empty-state">
                        <h2>{t("billing.noPending")}</h2>
                        <p>{t("billing.allPaid")}</p>
                    </div>
                ) : (
                    <div className="pending-bills-table-wrap">
                        <table className="products-table">
                            <thead>
                                <tr>
                                    <th>{t("billing.bill")}</th>
                                    <th>{t("billing.reference")}</th>
                                    <th>{t("billing.amount")}</th>
                                    <th><span className="sr-only">{t("common.actions")}</span></th>
                                </tr>
                            </thead>
                            <tbody>
                                {bills.map((bill) => (
                                    <tr key={bill.billId}>
                                        <td><strong>{bill.billNumber}</strong></td>
                                        <td>{bill.sessionId ? t("billing.referenceSession", { id: bill.sessionId }) : t("billing.referenceOrder", { id: bill.orderId })}</td>
                                        <td><strong>{formatCurrency(bill.totalAmount)}</strong></td>
                                        <td>
                                            <div className="product-row-actions">
                                                <button type="button" onClick={() => { setError(""); setSelectedBill(bill); }}>
                                                    {t("billing.pay")}
                                                </button>
                                                <button
                                                    type="button"
                                                    className="product-delete-button"
                                                    onClick={() => {
                                                        setError("");
                                                        setCancelTarget(bill);
                                                    }}
                                                >
                                                    {t("billing.cancel")}
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>

            {selectedBill && (
                <BillPaymentModal
                    bill={selectedBill}
                    loading={processing}
                    error={error}
                    onClose={() => setSelectedBill(null)}
                    onPay={handlePay}
                />
            )}

            {cancelTarget && (
                <CancelBillModal
                    bill={cancelTarget}
                    cancelling={processing}
                    error={error}
                    onClose={() => {
                        if (processing) return;
                        setCancelTarget(null);
                        setError("");
                    }}
                    onConfirm={() => handleCancel(cancelTarget)}
                />
            )}

            {receipt && (
                <ReceiptModal
                    bill={receipt}
                    refunding={refunding}
                    canRefund={canRefund}
                    onClose={() => setReceipt(null)}
                    onRefund={handleRefund}
                />
            )}
        </div>
    );
}
