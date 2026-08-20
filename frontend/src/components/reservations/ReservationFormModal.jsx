import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";
import { searchCustomers } from "../../api/customerApi";

const DURATION_OPTIONS = [30, 60, 90, 120];

export default function ReservationFormModal({ devices, saving, error, onClose, onSave }) {
    const { t } = useLanguage();
    const [customerName, setCustomerName] = useState("");
    const [customerPhone, setCustomerPhone] = useState("");
    const [deviceId, setDeviceId] = useState(devices[0]?.id ?? "");
    const [sessionType, setSessionType] = useState("SINGLE");
    const [startTime, setStartTime] = useState("");
    const [durationMinutes, setDurationMinutes] = useState(60);
    const [notes, setNotes] = useState("");

    const valid = customerName.trim() && customerPhone.trim() && deviceId && startTime;

    useEffect(() => {
        function handleKeyDown(event) {
            if (event.key === "Escape" && !saving) onClose();
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [onClose, saving]);

    async function handlePhoneBlur() {
        const phone = customerPhone.trim();
        if (!phone || customerName.trim()) return;

        try {
            const matches = await searchCustomers(phone);
            const exact = matches.find((customer) => customer.phone === phone);
            if (exact) setCustomerName(exact.name);
        } catch {
            // Lookup is a convenience only; ignore failures silently.
        }
    }

    function submit(event) {
        event.preventDefault();
        if (!valid || saving) return;
        onSave({
            customerName: customerName.trim(),
            customerPhone: customerPhone.trim(),
            deviceId: Number(deviceId),
            sessionType,
            startTime,
            durationMinutes: Number(durationMinutes),
            notes: notes.trim() || null,
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
                className="modal-container reservation-form-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="reservation-form-title"
                onSubmit={submit}
            >
                <div className="modal-header">
                    <div>
                        <span className="page-label">{t("nav.reservations")}</span>
                        <h2 id="reservation-form-title">{t("reservations.new")}</h2>
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

                <div className="product-form-grid">
                    <div>
                        <label htmlFor="reservation-customer-phone">{t("form.customerPhone")}</label>
                        <input
                            id="reservation-customer-phone"
                            autoFocus
                            maxLength="30"
                            value={customerPhone}
                            onChange={(event) => setCustomerPhone(event.target.value)}
                            onBlur={handlePhoneBlur}
                            placeholder={t("form.customerPhonePlaceholder")}
                        />
                    </div>
                    <div>
                        <label htmlFor="reservation-customer-name">{t("form.customerName")}</label>
                        <input
                            id="reservation-customer-name"
                            maxLength="100"
                            value={customerName}
                            onChange={(event) => setCustomerName(event.target.value)}
                            placeholder={t("form.customerNamePlaceholder")}
                        />
                    </div>
                </div>

                <div className="product-form-grid">
                    <div>
                        <label htmlFor="reservation-device">{t("reservations.device")}</label>
                        <select
                            id="reservation-device"
                            value={deviceId}
                            onChange={(event) => setDeviceId(event.target.value)}
                        >
                            {devices.map((device) => (
                                <option key={device.id} value={device.id}>
                                    {device.name} ({device.type})
                                </option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label htmlFor="reservation-session-type">{t("reservations.sessionType")}</label>
                        <select
                            id="reservation-session-type"
                            value={sessionType}
                            onChange={(event) => setSessionType(event.target.value)}
                        >
                            <option value="SINGLE">{t("modal.single")}</option>
                            <option value="MULTI">{t("modal.multi")}</option>
                            <option value="MATCH">{t("modal.match")}</option>
                        </select>
                    </div>
                </div>

                <div className="product-form-grid">
                    <div>
                        <label htmlFor="reservation-start-time">{t("reservations.startTime")}</label>
                        <input
                            id="reservation-start-time"
                            type="datetime-local"
                            value={startTime}
                            onChange={(event) => setStartTime(event.target.value)}
                        />
                    </div>
                    <div>
                        <label htmlFor="reservation-duration">{t("reservations.duration")}</label>
                        <select
                            id="reservation-duration"
                            value={durationMinutes}
                            onChange={(event) => setDurationMinutes(event.target.value)}
                        >
                            {DURATION_OPTIONS.map((minutes) => (
                                <option key={minutes} value={minutes}>
                                    {minutes} {t("reservations.minutes")}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                <label htmlFor="reservation-notes">{t("reservations.notes")}</label>
                <input
                    id="reservation-notes"
                    maxLength="300"
                    value={notes}
                    onChange={(event) => setNotes(event.target.value)}
                    placeholder={t("reservations.notesPlaceholder")}
                />

                {error && <div className="checkout-inline-error" role="alert">{error}</div>}

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
                        {saving ? t("common.working") : t("reservations.new")}
                    </button>
                </div>
            </form>
        </div>
    );
}
