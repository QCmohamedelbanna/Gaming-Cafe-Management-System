import { useState } from "react";

function money(value) {
    return `${Number(value || 0).toFixed(2)} EGP`;
}

export default function BillPaymentModal({ bill, loading, error, onClose, onPay }) {
    const [method, setMethod] = useState("CASH");
    const [tendered, setTendered] = useState("");
    const total = Number(bill.totalAmount || 0);
    const received = Number(tendered || 0);
    const valid = method !== "CASH" || received >= total;

    return (
        <div className="modal-overlay">
            <div className="modal-container bill-payment-modal" role="dialog" aria-modal="true">
                <div className="modal-header">
                    <div>
                        <span className="page-label">PENDING PAYMENT</span>
                        <h2>{bill.billNumber}</h2>
                        <p>{bill.sessionId ? `Session #${bill.sessionId}` : "Standalone order"}</p>
                    </div>
                    <button type="button" className="modal-close" disabled={loading} onClick={onClose}>
                        &times;
                    </button>
                </div>

                <div className="pending-bill-total">
                    <span>Total due</span>
                    <strong>{money(total)}</strong>
                </div>

                <span className="payment-section-label">Payment method</span>
                <div className="payment-method-grid">
                    {["CASH", "CARD", "MOBILE_WALLET"].map((value) => (
                        <button
                            type="button"
                            key={value}
                            className={method === value
                                ? "payment-method-button selected"
                                : "payment-method-button"}
                            onClick={() => setMethod(value)}
                        >
                            {value === "MOBILE_WALLET" ? "Mobile wallet" : value}
                        </button>
                    ))}
                </div>

                {method === "CASH" && (
                    <div className="cash-tendered-row">
                        <label htmlFor="pending-cash">Cash received</label>
                        <div>
                            <input
                                id="pending-cash"
                                type="number"
                                min={total.toFixed(2)}
                                step="0.01"
                                value={tendered}
                                onChange={(event) => setTendered(event.target.value)}
                                placeholder={total.toFixed(2)}
                            />
                            <span>EGP</span>
                        </div>
                    </div>
                )}

                {error && <div className="checkout-inline-error">{error}</div>}

                <div className="checkout-modal-actions">
                    <button type="button" className="back-button" disabled={loading} onClick={onClose}>
                        Cancel
                    </button>
                    <button
                        type="button"
                        className="primary-action"
                        disabled={loading || !valid}
                        onClick={() => onPay({
                            paymentMethod: method,
                            amountTendered: method === "CASH" ? received : null,
                        })}
                    >
                        {loading ? "Processing..." : `Pay ${money(total)}`}
                    </button>
                </div>
            </div>
        </div>
    );
}
