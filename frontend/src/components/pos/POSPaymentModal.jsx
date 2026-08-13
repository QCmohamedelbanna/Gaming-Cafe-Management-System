import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";

export default function POSPaymentModal({
    order,
    loading = false,
    error = "",
    onClose,
    onConfirm,
}) {
    const { t, formatCurrency } = useLanguage();
    const [paymentMethod, setPaymentMethod] = useState("CASH");
    const [amountTendered, setAmountTendered] = useState("");
    const total = Number(order?.totalAmount || 0);
    const subtotal = Number(order?.subtotalAmount || 0);
    const discount = Number(order?.discountAmount || 0);
    const received = Number(amountTendered || 0);
    const canConfirm = paymentMethod !== "CASH" || received >= total;

    useEffect(() => {
        function handleKeyDown(event) {
            if (event.key === "Escape" && !loading) onClose();
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [loading, onClose]);

    function submit(event) {
        event.preventDefault();
        if (!canConfirm || loading) return;
        onConfirm({
            paymentMethod,
            amountTendered: paymentMethod === "CASH" ? received : null,
        });
    }

    return (
        <div
            className="modal-overlay"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && !loading) onClose();
            }}
        >
            <form
                className="modal-container pos-payment-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="pos-payment-title"
                onSubmit={submit}
            >
                <div className="modal-header">
                    <div>
                        <span className="page-label">{t("pos.paymentLabel")}</span>
                        <h2 id="pos-payment-title">{t("pos.completePayment")}</h2>
                        <p>{t("pos.productsOnly", { id: order?.id })}</p>
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

                <div className="pos-payment-summary">
                    <div><span>{t("pos.subtotal")}</span><strong>{formatCurrency(subtotal)}</strong></div>
                    {discount > 0 && (
                        <div className="pos-discount-summary">
                        <span>{t("pos.discount")}</span><strong>-{formatCurrency(discount)}</strong>
                        </div>
                    )}
                    <div className="pos-payment-total"><span>{t("common.totalDue")}</span><strong>{formatCurrency(total)}</strong></div>
                </div>

                <div className="payment-method-section">
                    <span className="payment-section-label">{t("common.paymentMethod")}</span>
                    <div className="payment-method-grid">
                        {["CASH", "CARD", "MOBILE_WALLET"].map((method) => (
                            <button
                                type="button"
                                key={method}
                                className={paymentMethod === method
                                    ? "payment-method-button selected"
                                    : "payment-method-button"}
                                onClick={() => {
                                    setPaymentMethod(method);
                                    if (method !== "CASH") setAmountTendered("");
                                }}
                            >
                                {method === "MOBILE_WALLET" ? t("common.mobileWallet") : method === "CASH" ? t("common.cash") : t("common.card")}
                            </button>
                        ))}
                    </div>

                    {paymentMethod === "CASH" && (
                        <div className="cash-tendered-row">
                            <label htmlFor="pos-cash-received">{t("pos.amountReceived")}</label>
                            <div>
                                <input
                                    id="pos-cash-received"
                                    type="number"
                                    min={total.toFixed(2)}
                                    step="0.01"
                                    value={amountTendered}
                                    onChange={(event) => setAmountTendered(event.target.value)}
                                    placeholder={total.toFixed(2)}
                                    autoFocus
                                />
                                <span>{t("common.egp")}</span>
                            </div>
                        </div>
                    )}

                    {paymentMethod === "CASH" && received >= total && (
                        <div className="cash-change-preview">
                        {t("common.change")}: <strong>{formatCurrency(received - total)}</strong>
                        </div>
                    )}
                </div>

                {error && <div className="checkout-inline-error" role="alert">{error}</div>}

                <div className="checkout-modal-actions">
                    <button
                        type="button"
                        className="back-button"
                        disabled={loading}
                        onClick={onClose}
                    >
                        {t("pos.back")}
                    </button>
                    <button
                        type="submit"
                        className="primary-action"
                        disabled={loading || !canConfirm}
                    >
                        {loading ? t("modal.processing") : t("pos.payTotal", { amount: formatCurrency(total) })}
                    </button>
                </div>
            </form>
        </div>
    );
}
