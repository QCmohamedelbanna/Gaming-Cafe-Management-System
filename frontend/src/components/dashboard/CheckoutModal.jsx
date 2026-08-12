import { useEffect, useState } from "react";

function formatMoney(value) {
    return `${Number(value || 0).toFixed(2)} EGP`;
}

function calculateGamingAmount(session, now) {
    if (!session) return 0;

    if (session.finalAmount != null) {
        return Number(session.finalAmount);
    }

    if (session.sessionType === "MATCH") {
        return (
            Number(session.unitPriceSnapshot || 0) *
            Number(session.purchasedMatches || 1)
        );
    }

    if (!session.startTime) return 0;

    const elapsedSeconds = Math.max(
        0,
        (now - new Date(session.startTime).getTime()) / 1000
    );

    const billableSeconds = session.plannedMinutes == null
        ? elapsedSeconds
        : Math.min(elapsedSeconds, Number(session.plannedMinutes) * 60);

    const hourlyRate = Number(
        session.unitPriceSnapshot ||
        session.hourlyRateSnapshot ||
        0
    );

    return hourlyRate * (billableSeconds / 3600);
}

export default function CheckoutModal({
    device,
    session,
    order,
    finalBill = null,
    loading = false,
    error = "",
    onClose,
    onCheckout,
}) {
    const finalized = Boolean(finalBill);
    const items = finalized
        ? finalBill.lines ?? []
        : order?.items ?? [];
    const [now, setNow] = useState(Date.now());
    const [paymentMethod, setPaymentMethod] = useState("CASH");
    const [amountTendered, setAmountTendered] = useState("");

    const sessionAmount = finalized
        ? Number(finalBill.gamingAmount || 0)
        : calculateGamingAmount(session, now);

    const orderAmount = finalized
        ? Number(finalBill.orderAmount || 0)
        : Number(order?.totalAmount || 0);
    const grandTotal = finalized
        ? Number(finalBill.totalAmount || 0)
        : sessionAmount + orderAmount;
    const cashTendered = Number(amountTendered || 0);
    const canConfirm = paymentMethod !== "CASH"
        || cashTendered >= grandTotal;

    useEffect(() => {
        function handleKeyDown(event) {
            if (event.key === "Escape" && !loading) {
                onClose();
            }
        }

        window.addEventListener("keydown", handleKeyDown);

        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [loading, onClose]);

    useEffect(() => {
        if (finalized) return undefined;

        const interval = window.setInterval(
            () => setNow(Date.now()),
            1000
        );

        return () => window.clearInterval(interval);
    }, [finalized]);

    function handleOverlayClick(event) {
        if (event.target === event.currentTarget && !loading) {
            onClose();
        }
    }

    return (
        <div
            className="modal-overlay"
            role="presentation"
            onMouseDown={handleOverlayClick}
        >
            <div
                className="modal-container checkout-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="checkout-title"
            >
                <div className="modal-header">
                    <div>
                        <span className="page-label">
                            {finalized ? "FINAL CALCULATION" : "CHECKOUT"}
                        </span>
                        <h2 id="checkout-title">
                            {device?.name || session?.device?.name || "Session"}
                        </h2>
                        <p>
                            {finalized
                                ? "Session time stopped"
                                : session?.sessionType || "Gaming session"}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="modal-close"
                        aria-label="Close checkout"
                        disabled={loading}
                        onClick={onClose}
                    >
                        &times;
                    </button>
                </div>

                <div className="checkout-summary">
                    <div className="checkout-summary-row">
                        <span>Gaming cost</span>
                        <strong>{formatMoney(sessionAmount)}</strong>
                    </div>

                    <div className="checkout-order-section">
                        <div className="checkout-summary-row">
                            <span>Orders</span>
                            <strong>{formatMoney(orderAmount)}</strong>
                        </div>

                        {items.length > 0 && (
                            <div className="checkout-items">
                                {items.map((item) => (
                                    <div
                                        className="checkout-item"
                                        key={item.productId || item.id}
                                    >
                                        <div>
                                            <strong>
                                                {item.productName ||
                                                    item.product?.name ||
                                                    "Product"}
                                            </strong>
                                            <span>
                                                {item.quantity} × {formatMoney(
                                                    item.unitPrice ??
                                                    item.unitPriceSnapshot
                                                )}
                                            </span>
                                        </div>

                                        <strong>
                                            {formatMoney(item.lineTotal)}
                                        </strong>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    <div className="checkout-total">
                        <span>Total due</span>
                        <strong>{formatMoney(grandTotal)}</strong>
                    </div>

                    <div className="payment-method-section">
                        <span className="payment-section-label">Payment method</span>
                        <div className="payment-method-grid">
                            {["CASH", "CARD", "MOBILE_WALLET"].map((method) => (
                                <button
                                    type="button"
                                    key={method}
                                    className={paymentMethod === method
                                        ? "payment-method-button selected"
                                        : "payment-method-button"}
                                    onClick={() => setPaymentMethod(method)}
                                >
                                    {method === "MOBILE_WALLET"
                                        ? "Mobile wallet"
                                        : method}
                                </button>
                            ))}
                        </div>

                        {paymentMethod === "CASH" && (
                            <div className="cash-tendered-row">
                                <label htmlFor="cash-tendered">Cash received</label>
                                <div>
                                    <input
                                        id="cash-tendered"
                                        type="number"
                                        min={grandTotal.toFixed(2)}
                                        step="0.01"
                                        value={amountTendered}
                                        onChange={(event) =>
                                            setAmountTendered(event.target.value)
                                        }
                                        placeholder={grandTotal.toFixed(2)}
                                    />
                                    <span>EGP</span>
                                </div>
                            </div>
                        )}

                        {paymentMethod === "CASH" && cashTendered >= grandTotal && (
                            <div className="cash-change-preview">
                                Change: <strong>{formatMoney(cashTendered - grandTotal)}</strong>
                            </div>
                        )}
                    </div>

                    {error && (
                        <div className="checkout-inline-error" role="alert">
                            {error}
                        </div>
                    )}
                </div>

                <div className="checkout-modal-actions">
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
                        className="primary-action"
                        disabled={loading || !canConfirm}
                        onClick={() => onCheckout(finalBill.billId, {
                            paymentMethod,
                            amountTendered: paymentMethod === "CASH"
                                ? cashTendered
                                : null,
                        })}
                    >
                        {loading
                            ? "Processing..."
                            : `Confirm ${formatMoney(grandTotal)}`}
                    </button>
                </div>
            </div>
        </div>
    );
}
