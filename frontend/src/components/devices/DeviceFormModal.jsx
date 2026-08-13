import { useEffect, useState } from "react";

const DEVICE_STATUSES = ["AVAILABLE", "MAINTENANCE", "OFFLINE"];

export default function DeviceFormModal({ device, saving, onClose, onSave }) {
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
                        <span className="page-label">ADMIN DEVICES</span>
                        <h2 id="device-form-title">
                            {editing ? "Edit Device" : "Add Device"}
                        </h2>
                        <p>
                            {editing
                                ? "Update the station identity or operating state."
                                : "Register a PlayStation station for Operations."}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="modal-close"
                        aria-label="Close device form"
                        disabled={saving}
                        onClick={onClose}
                    >
                        &times;
                    </button>
                </div>

                <label htmlFor="device-name">Device name</label>
                <input
                    id="device-name"
                    autoFocus
                    maxLength="100"
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    placeholder="Example: PS5-3"
                />

                <div className="device-form-grid">
                    <div>
                        <label htmlFor="device-type">Console type</label>
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
                        <label htmlFor="device-status">Operating status</label>
                        <select
                            id="device-status"
                            value={status}
                            onChange={handleStatusChange}
                        >
                            {DEVICE_STATUSES.map((value) => (
                                <option key={value} value={value}>
                                    {value}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                <label htmlFor="maintenance-note">Maintenance note</label>
                <textarea
                    id="maintenance-note"
                    rows="3"
                    maxLength="500"
                    value={maintenanceNote}
                    onChange={(event) => setMaintenanceNote(event.target.value)}
                    disabled={status !== "MAINTENANCE"}
                    placeholder="Describe the issue or maintenance work"
                />

                <p className="device-form-hint">
                    Gaming prices are managed centrally in Pricing, not on the device.
                </p>

                <div className="product-form-actions">
                    <button
                        type="button"
                        className="product-secondary-button"
                        disabled={saving}
                        onClick={onClose}
                    >
                        Cancel
                    </button>
                    <button
                        type="submit"
                        className="primary-action"
                        disabled={!valid || saving}
                    >
                        {saving ? "Saving..." : editing ? "Save Changes" : "Add Device"}
                    </button>
                </div>
            </form>
        </div>
    );
}
