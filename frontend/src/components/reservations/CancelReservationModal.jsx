import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";

export default function CancelReservationModal({
    reservation,
    cancelling = false,
    error = "",
    onClose,
    onConfirm,
}) {
    const { t } = useLanguage();
    const [reason, setReason] = useState("");

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
                if (event.target === event.currentTarget && !cancelling) onClose();
            }}
        >
            <div
                className="modal-container cancel-order-modal"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="cancel-reservation-title"
                aria-describedby="cancel-reservation-description"
            >
                <div className="cancel-order-icon" aria-hidden="true">!</div>

                <div className="cancel-order-content">
                    <span className="page-label">{t("reservations.cancelLabel")}</span>
                    <h2 id="cancel-reservation-title">{t("reservations.cancelTitle")}</h2>
                    <p id="cancel-reservation-description">
                        {t("reservations.cancelDescription", { name: reservation.customer.name })}
                    </p>

                    <label htmlFor="cancel-reservation-reason">{t("reservations.cancelReasonLabel")}</label>
                    <input
                        id="cancel-reservation-reason"
                        maxLength="200"
                        value={reason}
                        onChange={(event) => setReason(event.target.value)}
                        placeholder={t("reservations.cancelReasonPlaceholder")}
                    />

                    {error && (
                        <div className="cancel-order-inline-error" role="alert">
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
                        {t("reservations.keepReservation")}
                    </button>

                    <button
                        type="button"
                        className="cancel-order-confirm-button"
                        disabled={cancelling}
                        onClick={() => onConfirm(reason.trim() || null)}
                    >
                        {cancelling ? t("pos.cancelling") : t("reservations.cancelAction")}
                    </button>
                </div>
            </div>
        </div>
    );
}
