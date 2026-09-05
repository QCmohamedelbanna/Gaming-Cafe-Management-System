import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";

const DEVICE_STATUSES = ["AVAILABLE", "MAINTENANCE", "OFFLINE"];
const CONTROL_PROVIDERS = ["NONE", "TUYA"];
const SHUTDOWN_POLICIES = ["NONE", "DIRECT_POWER"];

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
    const [provider, setProvider] = useState(device?.controlProvider ?? "NONE");
    const [controllerDeviceId, setControllerDeviceId] = useState(
        device?.controllerDeviceId ?? ""
    );
    const [controllerPowerCode, setControllerPowerCode] = useState(
        device?.controllerPowerCode ?? (device?.controlProvider === "TUYA" ? "switch_1" : "")
    );
    const [powerControlEnabled, setPowerControlEnabled] = useState(
        Boolean(device?.powerControlEnabled)
    );
    const [shutdownPolicy, setShutdownPolicy] = useState(
        device?.shutdownPolicy ?? "NONE"
    );
    const editing = Boolean(device);
    const valid = name.trim().length > 0
        && (provider !== "TUYA"
            || (controllerDeviceId.trim().length > 0
                && (!powerControlEnabled || controllerPowerCode.trim().length > 0)));

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

    function handleProviderChange(event) {
        const nextProvider = event.target.value;
        setProvider(nextProvider);
        if (nextProvider === "NONE") {
            setPowerControlEnabled(false);
            setShutdownPolicy("NONE");
        } else if (shutdownPolicy === "NONE") {
            setShutdownPolicy("DIRECT_POWER");
        }
        if (nextProvider === "TUYA" && !controllerPowerCode.trim()) {
            setControllerPowerCode("switch_1");
        }
    }

    function submit(event) {
        event.preventDefault();
        if (!valid || saving) return;

        onSave({
            device: {
                name: name.trim(),
                type,
                status,
                maintenanceNote: status === "MAINTENANCE"
                    ? maintenanceNote.trim() || null
                    : null,
            },
            control: {
                provider,
                controllerDeviceId: provider === "TUYA"
                    ? controllerDeviceId.trim() || null
                    : null,
                controllerPowerCode: provider === "TUYA"
                    ? controllerPowerCode.trim() || null
                    : null,
                enabled: provider === "TUYA" && powerControlEnabled,
                shutdownPolicy: provider === "TUYA" && powerControlEnabled
                    ? shutdownPolicy
                    : "NONE",
            },
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

                <div className="device-control-form-section">
                    <div className="device-control-form-heading">
                        <strong>{t("devices.hardwareControl")}</strong>
                        <span>{t("devices.hardwareControlHint")}</span>
                    </div>

                    <label htmlFor="device-control-provider">{t("devices.controlProvider")}</label>
                    <select
                        id="device-control-provider"
                        value={provider}
                        onChange={handleProviderChange}
                    >
                        {CONTROL_PROVIDERS.map((value) => (
                            <option key={value} value={value}>
                                {value === "TUYA" ? "TUYA" : t("devices.controlNone")}
                            </option>
                        ))}
                    </select>

                    {provider === "TUYA" && (
                        <>
                            <label htmlFor="tuya-device-id">{t("devices.tuyaDeviceId")}</label>
                            <input
                                id="tuya-device-id"
                                maxLength="255"
                                value={controllerDeviceId}
                                onChange={(event) => setControllerDeviceId(event.target.value)}
                                placeholder={t("devices.tuyaDeviceIdPlaceholder")}
                            />

                            <label htmlFor="tuya-power-code">{t("devices.powerCommandCode")}</label>
                            <input
                                id="tuya-power-code"
                                maxLength="100"
                                value={controllerPowerCode}
                                onChange={(event) => setControllerPowerCode(event.target.value)}
                                placeholder="switch_1"
                            />

                            <label className="device-control-checkbox" htmlFor="device-power-enabled">
                                <input
                                    id="device-power-enabled"
                                    type="checkbox"
                                    checked={powerControlEnabled}
                                    onChange={(event) => {
                                        const enabled = event.target.checked;
                                        setPowerControlEnabled(enabled);
                                        if (enabled && shutdownPolicy === "NONE") {
                                            setShutdownPolicy("DIRECT_POWER");
                                        }
                                    }}
                                />
                                <span>{t("devices.powerControlEnabled")}</span>
                            </label>

                            <label htmlFor="device-shutdown-policy">{t("devices.shutdownPolicy")}</label>
                            <select
                                id="device-shutdown-policy"
                                value={shutdownPolicy}
                                disabled={!powerControlEnabled}
                                onChange={(event) => setShutdownPolicy(event.target.value)}
                            >
                                {SHUTDOWN_POLICIES.map((value) => (
                                    <option key={value} value={value}>
                                        {value === "DIRECT_POWER"
                                            ? t("devices.directPower")
                                            : t("devices.controlNone")}
                                    </option>
                                ))}
                            </select>
                            <p className="device-form-hint">
                                {t("devices.directPowerHint")}
                            </p>
                        </>
                    )}
                </div>

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
