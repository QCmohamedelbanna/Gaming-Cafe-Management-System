import { useEffect } from "react";
import { useLanguage } from "../../i18n";

export default function CancelOrderModal({
    order,
    cancelling = false,
    error = "",
    onClose,
    onConfirm,
}) {
    const { t, formatCurrency, formatNumber, language } = useLanguage();
    useEffect(() => {
        function handleKeyDown(event) {
            if (event.key === "Escape" && !cancelling) onClose();
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [cancelling, onClose]);

    return (
        <div
            className="modal-overlay"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && !cancelling) {
                    onClose();
                }
            }}
        >
            <div
                className="modal-container cancel-order-modal"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="cancel-order-title"
                aria-describedby="cancel-order-description"
            >
                <div className="cancel-order-icon" aria-hidden="true">!</div>

                <div className="cancel-order-content">
                    <span className="page-label">{t("pos.cancelOrderLabel")}</span>
                    <h2 id="cancel-order-title">{t("pos.cancelOrderTitle")}</h2>
                    <p id="cancel-order-description">
                        {t("pos.cancelOrderQuestion", { id: order.id })}
                    </p>

                    <div className="cancel-order-summary">
                        <div>
                            <span>{t("common.orders")}</span>
                            <strong>#{order.id}</strong>
                        </div>
                        <div>
                            <span>{t("common.orders")}</span>
                            <strong>{t("pos.itemCount", { count: formatNumber(order.items?.length || 0), suffix: language === "ar" ? "" : order.items?.length === 1 ? "" : "s" })}</strong>
                        </div>
                        <div>
                            <span>{t("pos.total")}</span>
                            <strong>{formatCurrency(order.totalAmount)}</strong>
                        </div>
                    </div>

                    <div className="cancel-order-note">
                        {t("pos.cancelOrderNote")}
                    </div>

                    {error && (
                        <div className="cancel-order-inline-error" role="alert">
                            <strong>{t("pos.cancelOrderErrorTitle")}</strong>
                            <span>{error}</span>
                        </div>
                    )}
                </div>

                <div className="cancel-order-actions">
                    <button
                        type="button"
                        className="product-secondary-button"
                        disabled={cancelling}
                        onClick={onClose}
                    >
                        {t("pos.keepOrder")}
                    </button>

                    <button
                        type="button"
                        className="cancel-order-confirm-button"
                        disabled={cancelling}
                        onClick={onConfirm}
                    >
                        {cancelling ? t("pos.cancelling") : t("pos.cancelOrderAction")}
                    </button>
                </div>
            </div>
        </div>
    );
}
