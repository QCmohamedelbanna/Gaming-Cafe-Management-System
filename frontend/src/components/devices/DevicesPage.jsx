import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";
import {
    createDevice,
    deleteDevice,
    getDevices,
    getDevicePower,
    powerOnDevice,
    powerOffDevice,
    setDeviceActive,
    updateDevice,
} from "../../api/deviceApi";
import DeviceFormModal from "./DeviceFormModal";
import DeleteDeviceModal from "./DeleteDeviceModal";

function isActiveSession(device) {
    return device.status === "PLAYING";
}

function statusClass(status) {
    return `device-admin-status device-admin-status-${String(status).toLowerCase()}`;
}

export default function DevicesPage() {
    const { t } = useLanguage();
    const [devices, setDevices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [busyId, setBusyId] = useState(null);
    const [powerBusyId, setPowerBusyId] = useState(null);
    const [formDevice, setFormDevice] = useState(undefined);
    const [saving, setSaving] = useState(false);
    const [deleteTarget, setDeleteTarget] = useState(null);
    const [deleteError, setDeleteError] = useState("");
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    async function loadDevices() {
        try {
            setLoading(true);
            setError("");
            setDevices(await getDevices());
        } catch (loadError) {
            console.error(loadError);
            setError(loadError.message || t("devices.loadError"));
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadDevices();
    }, []);

    function showSuccess(text) {
        setError("");
        setMessage(text);
    }

    async function handleSave(data) {
        try {
            setSaving(true);
            setError("");
            const saved = formDevice
                ? await updateDevice(formDevice.id, data)
                : await createDevice(data);

            setDevices((current) => {
                const next = formDevice
                    ? current.map((item) => item.id === saved.id ? saved : item)
                    : [...current, saved];
                return next.sort((a, b) => {
                    const typeOrder = String(a.type).localeCompare(String(b.type));
                    return typeOrder || a.name.localeCompare(b.name);
                });
            });
            setFormDevice(undefined);
            showSuccess(t("devices.saved", { name: saved.name }));
        } catch (saveError) {
            console.error(saveError);
            setError(saveError.message || t("devices.saveError"));
        } finally {
            setSaving(false);
        }
    }

    async function handleToggle(device) {
        try {
            setBusyId(device.id);
            setMessage("");
            setError("");
            const updated = await setDeviceActive(device.id, device.active === false);
            setDevices((current) =>
                current.map((item) => item.id === updated.id ? updated : item)
            );
            showSuccess(
                updated.active
                    ? t("devices.activated", { name: updated.name })
                    : t("devices.deactivated", { name: updated.name })
            );
        } catch (toggleError) {
            console.error(toggleError);
            setError(toggleError.message || t("devices.saveError"));
        } finally {
            setBusyId(null);
        }
    }

    async function handleDelete(device) {
        try {
            setBusyId(device.id);
            setMessage("");
            setError("");
            setDeleteError("");
            await deleteDevice(device.id);
            setDevices((current) => current.filter((item) => item.id !== device.id));
            setDeleteTarget(null);
            showSuccess(t("devices.deleted", { name: device.name }));
        } catch (deleteError) {
            console.error(deleteError);
            const message = deleteError.message || t("devices.deleteError");
            setDeleteError(message);
            setError(message);
        } finally {
            setBusyId(null);
        }
    }

    async function handlePower(device, command) {
        try {
            setPowerBusyId(device.id);
            setMessage("");
            setError("");
            const result = command === "on"
                ? await powerOnDevice(device.id)
                : command === "off"
                    ? await powerOffDevice(device.id)
                    : await getDevicePower(device.id);
            setDevices((current) => current.map((item) => item.id === device.id
                ? {
                    ...item,
                    physicalPowerStatus: result.physicalState,
                    lastControlAt: result.timestamp,
                    lastControlError: result.success ? null : result.message,
                }
                : item));
            if (result.success) {
                showSuccess(`${device.name}: ${result.physicalState}`);
            } else {
                setError(result.message || t("devices.powerError"));
            }
        } catch (powerError) {
            console.error(powerError);
            setError(powerError.message || t("devices.powerError"));
        } finally {
            setPowerBusyId(null);
        }
    }

    return (
        <div className="devices-management-page">
            <div className="devices-management-header">
                <div>
                    <span className="page-label">{t("devices.pageLabel")}</span>
                    <h1>{t("devices.title")}</h1>
                    <p>{t("devices.descriptionShort")}</p>
                </div>

                <div className="devices-header-actions">
                    <button
                        type="button"
                        className="product-add-button"
                        onClick={() => setFormDevice(null)}
                    >
                        + {t("devices.addDevice")}
                    </button>
                </div>
            </div>

            {message && <div className="pricing-message">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}

            <div className="devices-management-note">
                <strong>{t("devices.pricingNoteTitle")}</strong>
                <span>{t("devices.pricingNote")}</span>
            </div>

            {loading ? (
                <p>{t("devices.loading")}</p>
            ) : devices.length === 0 ? (
                <div className="products-empty-state">
                    <h2>{t("devices.noDevices")}</h2>
                    <p>{t("devices.addFirst")}</p>
                </div>
            ) : (
                <div className="devices-table-wrap">
                    <table className="devices-table">
                        <thead>
                            <tr>
                                <th>{t("devices.name")}</th>
                                <th>{t("devices.type")}</th>
                                <th>{t("devices.status")}</th>
                                <th>{t("devices.availability")}</th>
                                <th>{t("devices.maintenanceNote")}</th>
                                <th>{t("devices.physicalPower")}</th>
                                <th><span className="sr-only">{t("common.actions")}</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            {devices.map((device) => {
                                const inUse = isActiveSession(device);
                                const busy = busyId === device.id;
                                const powerBusy = powerBusyId === device.id;
                                const active = device.active !== false;

                                return (
                                    <tr key={device.id}>
                                        <td>
                                            <strong>{device.name}</strong>
                                            {inUse && <small className="device-table-lock">{t("devices.activeSession")}</small>}
                                        </td>
                                        <td><span className="device-type-pill">{device.type}</span></td>
                                        <td>
                                            <span className={statusClass(device.status)}>
                                                {device.status === "AVAILABLE" ? t("devices.available") : device.status === "MAINTENANCE" ? t("devices.maintenance") : device.status === "OFFLINE" ? t("devices.offline") : device.status === "PLAYING" ? t("devices.playing") : device.status}
                                            </span>
                                        </td>
                                        <td>
                                            <span className={active
                                                ? "device-admin-active active"
                                                : "device-admin-active inactive"}
                                            >
                                                {active ? t("devices.yesActive") : t("devices.noActive")}
                                            </span>
                                        </td>
                                        <td>
                                            <span className="device-maintenance-note">
                                                {device.maintenanceNote || "—"}
                                            </span>
                                        </td>
                                        <td>
                                            {device.powerControlEnabled ? (
                                                <span className={`device-admin-active ${String(device.physicalPowerStatus || "UNKNOWN").toLowerCase()}`}>
                                                    {device.physicalPowerStatus || "UNKNOWN"}
                                                </span>
                                            ) : "—"}
                                        </td>
                                        <td>
                                            <div className="product-row-actions device-row-actions">
                                                {device.powerControlEnabled && (
                                                    <>
                                                        <button
                                                            type="button"
                                                            disabled={powerBusy}
                                                            onClick={() => handlePower(device, "on")}
                                                        >
                                                            {t("devices.powerOn")}
                                                        </button>
                                                        <button
                                                            type="button"
                                                            disabled={powerBusy}
                                                            onClick={() => handlePower(device, "off")}
                                                        >
                                                            {t("devices.powerOff")}
                                                        </button>
                                                        <button
                                                            type="button"
                                                            disabled={powerBusy}
                                                            onClick={() => handlePower(device, "status")}
                                                        >
                                                            {t("devices.powerRefresh")}
                                                        </button>
                                                    </>
                                                )}
                                                <button
                                                    type="button"
                                                    disabled={busy || inUse}
                                                    title={inUse ? t("devices.editLocked") : t("devices.editTitle")}
                                                    onClick={() => setFormDevice(device)}
                                                >
                                                    {t("common.edit")}
                                                </button>
                                                <button
                                                    type="button"
                                                    disabled={busy || inUse}
                                                    title={inUse
                                                        ? t("devices.availabilityLocked")
                                                        : t("devices.changeAvailability")}
                                                    onClick={() => handleToggle(device)}
                                                >
                                                    {active ? t("devices.deactivate") : t("devices.activate")}
                                                </button>
                                                <button
                                                    type="button"
                                                    className="product-delete-button"
                                                    disabled={busy || inUse}
                                                    title={inUse ? t("devices.deleteLocked") : t("devices.deleteTitle")}
                                                    onClick={() => {
                                                        setDeleteError("");
                                                        setDeleteTarget(device);
                                                    }}
                                                >
                                                    {busy ? t("common.working") : t("common.delete")}
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}

            {formDevice !== undefined && (
                <DeviceFormModal
                    device={formDevice}
                    saving={saving}
                    onClose={() => setFormDevice(undefined)}
                    onSave={handleSave}
                />
            )}

            {deleteTarget && (
                <DeleteDeviceModal
                    device={deleteTarget}
                    deleting={busyId === deleteTarget.id}
                    error={deleteError}
                    onClose={() => {
                        setDeleteTarget(null);
                        setDeleteError("");
                    }}
                    onConfirm={() => handleDelete(deleteTarget)}
                />
            )}
        </div>
    );
}
