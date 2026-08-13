import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";

export default function BillPaymentModal({ bill, loading, error, onClose, onPay }) {
    const { t, formatCurrency } = useLanguage();
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
                        <span className="page-label">{t("modal.pendingPayment")}</span>
                        <h2 id="bill-payment-title">{bill.billNumber}</h2>
                        <p>{bill.sessionId ? t("billing.referenceSession", { id: bill.sessionId }) : t("billing.standaloneOrder")}</p>
                    </div>

                    <button
                        type="button"
                        className="modal-close"
                        aria-label={t("billing.closePayment")}
                        disabled={loading}
                        onClick={onClose}
                    >
                        &times;
                    </button>
                </div>

                <div className="pending-bill-total">
                    <span>{t("common.totalDue")}</span>
                    <strong>{formatCurrency(total)}</strong>
                </div>

                <div className="payment-method-section">
                    <span className="payment-section-label">{t("common.paymentMethod")}</span>

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
                                {value === "MOBILE_WALLET" ? t("common.mobileWallet") : value === "CASH" ? t("common.cash") : t("common.card")}
                            </button>
                        ))}
                    </div>

                    {method === "CASH" && (
                        <div className="cash-tendered-row">
                            <label htmlFor="pending-cash">{t("common.cashReceived")}</label>
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
                                <span>{t("common.egp")}</span>
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
                        {t("common.cancel")}
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
                        {loading ? t("modal.processing") : t("billing.payTotal", { amount: formatCurrency(total) })}
                    </button>
                </div>
            </div>
        </div>
    );
}
