import { useEffect, useState } from "react";
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
            setError(loadError.message || "Could not load pending bills.");
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
            setMessage(`Bill ${paid.billNumber} paid successfully.`);
        } catch (payError) {
            console.error(payError);
            setError(payError.message || "Could not pay bill.");
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
            setMessage(`${bill.billNumber} cancelled.`);
        } catch (cancelError) {
            console.error(cancelError);
            setError(cancelError.message || "Could not cancel bill.");
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
            setError(refundError.message || "Could not refund bill.");
        } finally {
            setRefunding(false);
        }
    }

    return (
        <div className="billing-page">
            <div className="billing-page-header">
                <div>
                    <span className="page-label">CASHIER</span>
                    <h1>Billing</h1>
                    <p>Settle bills created by automatic session expiry.</p>
                </div>
                <button type="button" className="refresh-button" onClick={loadBills}>
                    Refresh
                </button>
            </div>

            {message && <div className="pricing-message">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}

            <section className="pending-bills-section">
                <div className="pending-bills-heading">
                    <div>
                        <span className="page-label">ACTION REQUIRED</span>
                        <h2>Pending bills</h2>
                    </div>
                    <span className="pending-bills-count">{bills.length}</span>
                </div>

                {loading ? (
                    <p>Loading pending bills...</p>
                ) : bills.length === 0 ? (
                    <div className="products-empty-state">
                        <h2>No pending bills</h2>
                        <p>All completed sessions have been paid.</p>
                    </div>
                ) : (
                    <div className="pending-bills-table-wrap">
                        <table className="products-table">
                            <thead>
                                <tr>
                                    <th>Bill</th>
                                    <th>Reference</th>
                                    <th>Amount</th>
                                    <th><span className="sr-only">Actions</span></th>
                                </tr>
                            </thead>
                            <tbody>
                                {bills.map((bill) => (
                                    <tr key={bill.billId}>
                                        <td><strong>{bill.billNumber}</strong></td>
                                        <td>{bill.sessionId ? `Session #${bill.sessionId}` : `Order #${bill.orderId}`}</td>
                                        <td><strong>{Number(bill.totalAmount).toFixed(2)} EGP</strong></td>
                                        <td>
                                            <div className="product-row-actions">
                                                <button type="button" onClick={() => { setError(""); setSelectedBill(bill); }}>
                                                    Pay
                                                </button>
                                                <button
                                                    type="button"
                                                    className="product-delete-button"
                                                    onClick={() => {
                                                        setError("");
                                                        setCancelTarget(bill);
                                                    }}
                                                >
                                                    Cancel
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
                    onClose={() => setReceipt(null)}
                    onRefund={handleRefund}
                />
            )}
        </div>
    );
}
