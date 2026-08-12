import { useEffect, useState } from "react";

function money(value) {
    return `${Number(value || 0).toFixed(2)} EGP`;
}

export default function ReceiptModal({
    bill,
    refunding = false,
    onClose,
    onRefund = null,
}) {
    const [refundOpen, setRefundOpen] = useState(false);
    const [reason, setReason] = useState("");
    const refundable = bill.status === "PAID" && Boolean(onRefund);

    useEffect(() => {
        function handleKeyDown(event) {
            if (event.key === "Escape" && !refunding) onClose();
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [onClose, refunding]);

    function submitRefund(event) {
        event.preventDefault();
        if (!reason.trim() || refunding) return;
        onRefund(reason.trim());
    }

    return (
        <div
            className="modal-overlay receipt-overlay"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && !refunding) onClose();
            }}
        >
            <div
                className="modal-container receipt-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="receipt-title"
            >
                <div className="modal-header receipt-header no-print">
                    <div>
                        <span className="page-label">PAYMENT COMPLETE</span>
                        <h2 id="receipt-title">Receipt</h2>
                        <p>{bill.billNumber}</p>
                    </div>
                    <button
                        type="button"
                        className="modal-close"
                        disabled={refunding}
                        onClick={onClose}
                        aria-label="Close receipt"
                    >
                        &times;
                    </button>
                </div>

                <div className="receipt-print-area">
                    <div className="receipt-brand">Gaming Cafe</div>
                    <div className="receipt-number">{bill.billNumber}</div>

                    {bill.lines?.length > 0 && (
                        <div className="receipt-lines">
                            {bill.lines.map((line) => (
                                <div className="receipt-line" key={line.productId}>
                                    <span>
                                        {line.productName} × {line.quantity}
                                    </span>
                                    <strong>{money(line.lineTotal)}</strong>
                                </div>
                            ))}
                        </div>
                    )}

                    <div className="receipt-amounts">
                        <div><span>Gaming</span><strong>{money(bill.gamingAmount)}</strong></div>
                        <div><span>Products</span><strong>{money(bill.orderAmount)}</strong></div>
                        <div className="receipt-grand-total"><span>Total</span><strong>{money(bill.totalAmount)}</strong></div>
                    </div>

                    {bill.paymentMethod && (
                        <div className="receipt-payment-details">
                            <div><span>Payment</span><strong>{bill.paymentMethod.replace("_", " ")}</strong></div>
                            <div><span>Received</span><strong>{money(bill.amountTendered)}</strong></div>
                            <div><span>Change</span><strong>{money(bill.changeAmount)}</strong></div>
                        </div>
                    )}

                    <div className={`receipt-status ${String(bill.status).toLowerCase()}`}>
                        {String(bill.status).replace("_", " ")}
                    </div>
                </div>

                {refundOpen && refundable && (
                    <form className="refund-form no-print" onSubmit={submitRefund}>
                        <label htmlFor="refund-reason">Refund reason</label>
                        <textarea
                            id="refund-reason"
                            rows="3"
                            value={reason}
                            onChange={(event) => setReason(event.target.value)}
                            placeholder="Explain why this bill is being refunded"
                            autoFocus
                        />
                        <div className="refund-form-actions">
                            <button
                                type="button"
                                className="product-secondary-button"
                                disabled={refunding}
                                onClick={() => setRefundOpen(false)}
                            >
                                Keep bill
                            </button>
                            <button
                                type="submit"
                                className="confirm-delete-product-button"
                                disabled={refunding || !reason.trim()}
                            >
                                {refunding ? "Refunding..." : "Confirm refund"}
                            </button>
                        </div>
                    </form>
                )}

                <div className="receipt-actions no-print">
                    <button type="button" className="product-secondary-button" onClick={() => window.print()}>
                        Print receipt
                    </button>
                    {refundable && !refundOpen && (
                        <button
                            type="button"
                            className="receipt-refund-button"
                            disabled={refunding}
                            onClick={() => setRefundOpen(true)}
                        >
                            Refund bill
                        </button>
                    )}
                    <button type="button" className="primary-action" onClick={onClose}>
                        Done
                    </button>
                </div>
            </div>
        </div>
    );
}
