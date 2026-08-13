import { useEffect, useState } from "react";
import {
    createDevice,
    deleteDevice,
    getDevices,
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
    const [devices, setDevices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [busyId, setBusyId] = useState(null);
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
            setError(loadError.message || "Could not load devices.");
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
            showSuccess(`${saved.name} saved successfully.`);
        } catch (saveError) {
            console.error(saveError);
            setError(saveError.message || "Could not save device.");
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
                `${updated.name} ${updated.active ? "activated" : "deactivated"}.`
            );
        } catch (toggleError) {
            console.error(toggleError);
            setError(toggleError.message || "Could not update device availability.");
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
            showSuccess(`${device.name} deleted.`);
        } catch (deleteError) {
            console.error(deleteError);
            const message = deleteError.message || "Could not delete device.";
            setDeleteError(message);
            setError(message);
        } finally {
            setBusyId(null);
        }
    }

    return (
        <div className="devices-management-page">
            <div className="devices-management-header">
                <div>
                    <span className="page-label">ADMIN</span>
                    <h1>Devices</h1>
                    <p>Manage gaming stations, availability, and maintenance status.</p>
                </div>

                <div className="devices-header-actions">
                    <button type="button" className="refresh-button" onClick={loadDevices}>
                        Refresh
                    </button>
                    <button
                        type="button"
                        className="product-add-button"
                        onClick={() => setFormDevice(null)}
                    >
                        + Add Device
                    </button>
                </div>
            </div>

            {message && <div className="pricing-message">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}

            <div className="devices-management-note">
                <strong>Pricing is separate from device management.</strong>
                <span>Set gaming prices by console and session type in Pricing.</span>
            </div>

            {loading ? (
                <p>Loading devices...</p>
            ) : devices.length === 0 ? (
                <div className="products-empty-state">
                    <h2>No devices yet</h2>
                    <p>Add a PS4 or PS5 station to make it available in Operations.</p>
                </div>
            ) : (
                <div className="devices-table-wrap">
                    <table className="devices-table">
                        <thead>
                            <tr>
                                <th>Device</th>
                                <th>Type</th>
                                <th>Status</th>
                                <th>Availability</th>
                                <th>Maintenance note</th>
                                <th><span className="sr-only">Actions</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            {devices.map((device) => {
                                const inUse = isActiveSession(device);
                                const busy = busyId === device.id;
                                const active = device.active !== false;

                                return (
                                    <tr key={device.id}>
                                        <td>
                                            <strong>{device.name}</strong>
                                            {inUse && <small className="device-table-lock">Active session</small>}
                                        </td>
                                        <td><span className="device-type-pill">{device.type}</span></td>
                                        <td>
                                            <span className={statusClass(device.status)}>
                                                {device.status}
                                            </span>
                                        </td>
                                        <td>
                                            <span className={active
                                                ? "device-admin-active active"
                                                : "device-admin-active inactive"}
                                            >
                                                {active ? "ACTIVE" : "INACTIVE"}
                                            </span>
                                        </td>
                                        <td>
                                            <span className="device-maintenance-note">
                                                {device.maintenanceNote || "—"}
                                            </span>
                                        </td>
                                        <td>
                                            <div className="product-row-actions device-row-actions">
                                                <button
                                                    type="button"
                                                    disabled={busy || inUse}
                                                    title={inUse ? "Editing is disabled during an active session" : "Edit device"}
                                                    onClick={() => setFormDevice(device)}
                                                >
                                                    Edit
                                                </button>
                                                <button
                                                    type="button"
                                                    disabled={busy || inUse}
                                                    title={inUse
                                                        ? "Availability is locked during an active session"
                                                        : "Change availability"}
                                                    onClick={() => handleToggle(device)}
                                                >
                                                    {active ? "Deactivate" : "Activate"}
                                                </button>
                                                <button
                                                    type="button"
                                                    className="product-delete-button"
                                                    disabled={busy || inUse}
                                                    title={inUse ? "Deleting is disabled during an active session" : "Delete device"}
                                                    onClick={() => {
                                                        setDeleteError("");
                                                        setDeleteTarget(device);
                                                    }}
                                                >
                                                    {busy ? "Working..." : "Delete"}
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
