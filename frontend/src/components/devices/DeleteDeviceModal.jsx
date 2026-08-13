import { useEffect } from "react";
import { useLanguage } from "../../i18n";

export default function DeleteDeviceModal({
    device,
    deleting,
    error,
    onClose,
    onConfirm,
}) {
    const { t } = useLanguage();
    useEffect(() => {
        function handleKeyDown(event) {
            if (event.key === "Escape" && !deleting) onClose();
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [deleting, onClose]);

    return (
        <div
            className="modal-overlay"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && !deleting) onClose();
            }}
        >
            <div
                className="modal-container delete-device-modal"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="delete-device-title"
                aria-describedby="delete-device-description"
            >
                <div className="delete-product-icon" aria-hidden="true">!</div>

                <div className="delete-product-content">
                    <span className="page-label">{t("devices.removeLabel")}</span>
                    <h2 id="delete-device-title">{t("devices.deleteTitle")}</h2>
                    <p id="delete-device-description">
                        {t("devices.deleteDescription", { name: device.name })}
                    </p>

                    <div className="delete-product-summary">
                        <span>{device.type} station</span>
                        <strong>{device.status === "AVAILABLE" ? t("devices.available") : device.status === "MAINTENANCE" ? t("devices.maintenance") : device.status === "OFFLINE" ? t("devices.offline") : device.status}</strong>
                    </div>

                    <div className="delete-product-note">
                        {t("devices.deleteNote")}
                    </div>

                    {error && (
                        <div className="delete-product-inline-error" role="alert">
                            <strong>{t("devices.deleteErrorTitle")}</strong>
                            <span>{error}</span>
                        </div>
                    )}
                </div>

                <div className="delete-product-actions">
                    <button
                        type="button"
                        className="product-secondary-button"
                        disabled={deleting}
                        onClick={onClose}
                    >
                        {t("common.cancel")}
                    </button>
                    <button
                        type="button"
                        className="confirm-delete-product-button"
                        disabled={deleting}
                        onClick={onConfirm}
                    >
                        {deleting ? t("devices.deleting") : t("devices.deleteDevice")}
                    </button>
                </div>
            </div>
        </div>
    );
}
