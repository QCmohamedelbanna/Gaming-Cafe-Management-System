import { useEffect } from "react";
import { useLanguage } from "../../i18n";

export default function CancelBillModal({
    bill,
    cancelling,
    error,
    onClose,
    onConfirm,
}) {
    const { t, formatCurrency } = useLanguage();
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
                className="modal-container cancel-bill-modal"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="cancel-bill-title"
                aria-describedby="cancel-bill-description"
            >
                <div className="cancel-bill-icon" aria-hidden="true">!</div>

                <div className="cancel-bill-content">
                    <span className="page-label">{t("modal.cancelBillLabel")}</span>
                    <h2 id="cancel-bill-title">{t("billing.cancelBillTitle")}</h2>
                    <p id="cancel-bill-description">
                        {t("billing.cancelBillQuestion", { bill: bill.billNumber })}
                    </p>

                    <div className="cancel-bill-summary">
                        <div>
                            <span>{t("modal.bill")}</span>
                            <strong>{bill.billNumber}</strong>
                        </div>
                        <div>
                            <span>{t("modal.reference")}</span>
                            <strong>
                                {bill.sessionId
                                    ? t("billing.referenceSession", { id: bill.sessionId })
                                    : t("billing.referenceOrder", { id: bill.orderId })}
                            </strong>
                        </div>
                        <div>
                            <span>{t("modal.amount")}</span>
                            <strong>{formatCurrency(bill.totalAmount)}</strong>
                        </div>
                    </div>

                    <div className="cancel-bill-note">
                        {t("billing.cancelBillNote")}
                    </div>

                    {error && (
                        <div className="cancel-bill-inline-error" role="alert">
                            <strong>{t("billing.cancelErrorTitle")}</strong>
                            <span>{error}</span>
                        </div>
                    )}
                </div>

                <div className="cancel-bill-actions">
                    <button
                        type="button"
                        className="product-secondary-button"
                        disabled={cancelling}
                        onClick={onClose}
                    >
                        {t("billing.keepBill")}
                    </button>

                    <button
                        type="button"
                        className="cancel-bill-confirm-button"
                        disabled={cancelling}
                        onClick={onConfirm}
                    >
                        {cancelling ? t("billing.cancelling") : t("billing.cancelBillAction")}
                    </button>
                </div>
            </div>
        </div>
    );
}
