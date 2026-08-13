import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";

const DEVICE_STATUSES = ["AVAILABLE", "MAINTENANCE", "OFFLINE"];

export default function DeviceFormModal({ device, saving, onClose, onSave }) {
    const { t } = useLanguage();
    const [name, setName] = useState(device?.name ?? "");
    const [type, setType] = useState(device?.type ?? "PS4");
    const [status, setStatus] = useState(
        DEVICE_STATUSES.includes(device?.status) ? device.status : "AVAILABLE"
    );
    const [maintenanceNote, setMaintenanceNote] = useState(
        device?.maintenanceNote ?? ""
    );
    const editing = Boolean(device);
    const valid = name.trim().length > 0;

    useEffect(() => {
        function handleKeyDown(event) {
            if (event.key === "Escape" && !saving) onClose();
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [onClose, saving]);

    function handleStatusChange(event) {
        const nextStatus = event.target.value;
        setStatus(nextStatus);
        if (nextStatus !== "MAINTENANCE") {
            setMaintenanceNote("");
        }
    }

    function submit(event) {
        event.preventDefault();
        if (!valid || saving) return;

        onSave({
            name: name.trim(),
            type,
            status,
            maintenanceNote: status === "MAINTENANCE"
                ? maintenanceNote.trim() || null
                : null,
        });
    }

    return (
        <div
            className="modal-overlay"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && !saving) onClose();
            }}
        >
            <form
                className="modal-container device-form-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="device-form-title"
                onSubmit={submit}
            >
                <div className="modal-header">
                    <div>
                        <span className="page-label">{t("devices.formLabel")}</span>
                        <h2 id="device-form-title">
                            {editing ? t("devices.editDevice") : t("devices.addDevice")}
                        </h2>
                        <p>
                            {editing
                                ? t("devices.updateTitle")
                                : t("devices.createTitle")}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="modal-close"
                        aria-label={t("common.close")}
                        disabled={saving}
                        onClick={onClose}
                    >
                        &times;
                    </button>
                </div>

                <label htmlFor="device-name">{t("devices.deviceName")}</label>
                <input
                    id="device-name"
                    autoFocus
                    maxLength="100"
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    placeholder={t("devices.exampleName")}
                />

                <div className="device-form-grid">
                    <div>
                        <label htmlFor="device-type">{t("devices.consoleType")}</label>
                        <select
                            id="device-type"
                            value={type}
                            onChange={(event) => setType(event.target.value)}
                        >
                            <option value="PS4">PS4</option>
                            <option value="PS5">PS5</option>
                        </select>
                    </div>

                    <div>
                        <label htmlFor="device-status">{t("devices.operatingStatus")}</label>
                        <select
                            id="device-status"
                            value={status}
                            onChange={handleStatusChange}
                        >
                            {DEVICE_STATUSES.map((value) => (
                                <option key={value} value={value}>
                                    {value === "AVAILABLE" ? t("devices.available") : value === "MAINTENANCE" ? t("devices.maintenance") : t("devices.offline")}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                <label htmlFor="maintenance-note">{t("devices.maintenanceNote")}</label>
                <textarea
                    id="maintenance-note"
                    rows="3"
                    maxLength="500"
                    value={maintenanceNote}
                    onChange={(event) => setMaintenanceNote(event.target.value)}
                    disabled={status !== "MAINTENANCE"}
                    placeholder={t("devices.maintenancePlaceholder")}
                />

                <p className="device-form-hint">
                    {t("devices.formHint")}
                </p>

                <div className="product-form-actions">
                    <button
                        type="button"
                        className="product-secondary-button"
                        disabled={saving}
                        onClick={onClose}
                    >
                        {t("common.cancel")}
                    </button>
                    <button
                        type="submit"
                        className="primary-action"
                        disabled={!valid || saving}
                    >
                        {saving ? t("devices.saving") : editing ? t("devices.saveChanges") : t("devices.addDevice")}
                    </button>
                </div>
            </form>
        </div>
    );
}
