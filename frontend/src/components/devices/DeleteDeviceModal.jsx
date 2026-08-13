import { useEffect } from "react";

export default function DeleteDeviceModal({
    device,
    deleting,
    error,
    onClose,
    onConfirm,
}) {
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
                    <span className="page-label">REMOVE FROM OPERATIONS</span>
                    <h2 id="delete-device-title">Delete device?</h2>
                    <p id="delete-device-description">
                        <strong>{device.name}</strong> will be removed from active devices.
                        Historical session records will be preserved.
                    </p>

                    <div className="delete-product-summary">
                        <span>{device.type} station</span>
                        <strong>{device.status}</strong>
                    </div>

                    <div className="delete-product-note">
                        A device can be removed after its active session ends. Its
                        session history remains available for billing and reporting.
                        Devices with active sessions are protected automatically.
                    </div>

                    {error && (
                        <div className="delete-product-inline-error" role="alert">
                            <strong>Could not delete this device</strong>
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
                        Cancel
                    </button>
                    <button
                        type="button"
                        className="confirm-delete-product-button"
                        disabled={deleting}
                        onClick={onConfirm}
                    >
                        {deleting ? "Deleting..." : "Delete Device"}
                    </button>
                </div>
            </div>
        </div>
    );
}
