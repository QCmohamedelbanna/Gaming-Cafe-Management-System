import { useEffect, useState } from "react";

function money(value) {
    return `${Number(value || 0).toFixed(2)} EGP`;
}

export default function BillPaymentModal({ bill, loading, error, onClose, onPay }) {
    const [method, setMethod] = useState("CASH");
    const [tendered, setTendered] = useState("");
    const total = Number(bill.totalAmount || 0);
    const received = Number(tendered || 0);
    const valid = method !== "CASH" || received >= total;

    useEffect(() => {
        function handleKeyDown(event) {
            if (event.key === "Escape" && !loading) {
                onClose();
            }
        }

        window.addEventListener("keydown", handleKeyDown);

        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [loading, onClose]);

    function handleOverlayMouseDown(event) {
        if (event.target === event.currentTarget && !loading) {
            onClose();
        }
    }

    return (
        <div
            className="modal-overlay"
            role="presentation"
            onMouseDown={handleOverlayMouseDown}
        >
            <div
                className="modal-container bill-payment-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="bill-payment-title"
            >
                <div className="modal-header">
                    <div>
                        <span className="page-label">PENDING PAYMENT</span>
                        <h2 id="bill-payment-title">{bill.billNumber}</h2>
                        <p>{bill.sessionId ? `Session #${bill.sessionId}` : "Standalone order"}</p>
                    </div>

                    <button
                        type="button"
                        className="modal-close"
                        aria-label="Close payment dialog"
                        disabled={loading}
                        onClick={onClose}
                    >
                        &times;
                    </button>
                </div>

                <div className="pending-bill-total">
                    <span>Total due</span>
                    <strong>{money(total)}</strong>
                </div>

                <div className="payment-method-section">
                    <span className="payment-section-label">Payment method</span>

                    <div className="payment-method-grid">
                        {["CASH", "CARD", "MOBILE_WALLET"].map((value) => (
                            <button
                                type="button"
                                key={value}
                                className={method === value
                                    ? "payment-method-button selected"
                                    : "payment-method-button"}
                                aria-pressed={method === value}
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
                </div>

                {error && <div className="checkout-inline-error">{error}</div>}

                <div className="checkout-modal-actions bill-payment-actions">
                    <button
                        type="button"
                        className="back-button"
                        disabled={loading}
                        onClick={onClose}
                    >
                        Cancel
                    </button>

                    <button
                        type="button"
                        className="primary-action bill-payment-submit"
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
