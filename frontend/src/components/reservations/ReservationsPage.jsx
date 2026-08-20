import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";
import { getDevices } from "../../api/deviceApi";
import { getPricing } from "../../api/pricingApi";
import {
    cancelReservation,
    checkInReservation,
    createReservation,
    getUpcomingReservations,
} from "../../api/reservationApi";
import ReservationFormModal from "./ReservationFormModal";
import CancelReservationModal from "./CancelReservationModal";
import StartSessionModal from "../dashboard/StartSessionModal";

function formatDateTime(value, language) {
    return new Date(value).toLocaleString(language === "ar" ? "ar-EG" : "en-EG", {
        dateStyle: "medium",
        timeStyle: "short",
    });
}

export default function ReservationsPage() {
    const { t, language } = useLanguage();
    const [reservations, setReservations] = useState([]);
    const [devices, setDevices] = useState([]);
    const [pricing, setPricing] = useState([]);
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const [formOpen, setFormOpen] = useState(false);
    const [saving, setSaving] = useState(false);
    const [formError, setFormError] = useState("");

    const [checkInTarget, setCheckInTarget] = useState(null);
    const [checkingIn, setCheckingIn] = useState(false);
    const [checkInError, setCheckInError] = useState("");

    const [cancelTarget, setCancelTarget] = useState(null);
    const [cancelling, setCancelling] = useState(false);
    const [cancelError, setCancelError] = useState("");

    async function loadAll() {
        try {
            setLoading(true);
            setError("");
            const [reservationList, deviceList, pricingList] = await Promise.all([
                getUpcomingReservations(),
                getDevices(),
                getPricing(),
            ]);
            setReservations(reservationList);
            setDevices(deviceList);
            setPricing(pricingList);
        } catch (loadError) {
            console.error(loadError);
            setError(loadError.message || t("reservations.loadError"));
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadAll();
    }, []);

    function showSuccess(text) {
        setError("");
        setMessage(text);
    }

    async function handleCreate(data) {
        try {
            setSaving(true);
            setFormError("");
            const created = await createReservation(data);
            setReservations((current) => [...current, created].sort(
                (a, b) => new Date(a.startTime) - new Date(b.startTime)
            ));
            setFormOpen(false);
            showSuccess(t("reservations.created", { name: created.customer.name }));
        } catch (saveError) {
            console.error(saveError);
            setFormError(saveError.message || t("reservations.createError"));
        } finally {
            setSaving(false);
        }
    }

    async function handleCheckIn(payload) {
        if (!checkInTarget) return;

        try {
            setCheckingIn(true);
            setCheckInError("");
            const updated = await checkInReservation(checkInTarget.id, payload);
            setReservations((current) => current.filter((item) => item.id !== updated.id));
            setCheckInTarget(null);
            showSuccess(t("reservations.checkedIn", { name: updated.customer.name }));
        } catch (checkInErr) {
            console.error(checkInErr);
            setCheckInError(checkInErr.message || t("reservations.checkInError"));
        } finally {
            setCheckingIn(false);
        }
    }

    async function handleCancel(reason) {
        if (!cancelTarget) return;

        try {
            setCancelling(true);
            setCancelError("");
            const updated = await cancelReservation(cancelTarget.id, reason);
            setReservations((current) => current.filter((item) => item.id !== updated.id));
            setCancelTarget(null);
            showSuccess(t("reservations.cancelled", { name: updated.customer.name }));
        } catch (cancelErr) {
            console.error(cancelErr);
            setCancelError(cancelErr.message || t("reservations.cancelError"));
        } finally {
            setCancelling(false);
        }
    }

    return (
        <div className="products-management-page">
            <div className="products-management-header">
                <div>
                    <span className="page-label">{t("reservations.pageLabel")}</span>
                    <h1>{t("reservations.title")}</h1>
                    <p>{t("reservations.description")}</p>
                </div>
                <button
                    type="button"
                    className="product-add-button"
                    disabled={devices.length === 0}
                    onClick={() => setFormOpen(true)}
                >
                    + {t("reservations.new")}
                </button>
            </div>

            {message && <div className="pricing-message">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}

            {loading ? (
                <p>{t("reservations.loading")}</p>
            ) : reservations.length === 0 ? (
                <div className="products-empty-state">
                    <h2>{t("reservations.noUpcoming")}</h2>
                    <p>{t("reservations.addFirst")}</p>
                </div>
            ) : (
                <div className="products-table-wrap">
                    <table className="products-table">
                        <thead>
                            <tr>
                                <th>{t("reservations.startTime")}</th>
                                <th>{t("reservations.customer")}</th>
                                <th>{t("reservations.device")}</th>
                                <th>{t("reservations.sessionType")}</th>
                                <th><span className="sr-only">{t("common.actions")}</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            {reservations.map((reservation) => (
                                <tr key={reservation.id}>
                                    <td><strong>{formatDateTime(reservation.startTime, language)}</strong></td>
                                    <td>
                                        <strong>{reservation.customer.name}</strong>
                                        <div>{reservation.customer.phone}</div>
                                    </td>
                                    <td>{reservation.device.name}</td>
                                    <td>
                                        {reservation.sessionType === "SINGLE" ? t("modal.single")
                                            : reservation.sessionType === "MULTI" ? t("modal.multi")
                                                : t("modal.match")}
                                    </td>
                                    <td>
                                        <div className="product-row-actions">
                                            <button onClick={() => setCheckInTarget(reservation)}>
                                                {t("reservations.checkIn")}
                                            </button>
                                            <button
                                                className="product-delete-button"
                                                onClick={() => setCancelTarget(reservation)}
                                            >
                                                {t("reservations.cancelAction")}
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {formOpen && (
                <ReservationFormModal
                    devices={devices}
                    saving={saving}
                    error={formError}
                    onClose={() => setFormOpen(false)}
                    onSave={handleCreate}
                />
            )}

            {checkInTarget && (
                <StartSessionModal
                    device={checkInTarget.device}
                    pricing={pricing}
                    loading={checkingIn}
                    onClose={() => setCheckInTarget(null)}
                    onStart={handleCheckIn}
                />
            )}
            {checkInError && checkInTarget && (
                <div className="checkout-inline-error" role="alert">{checkInError}</div>
            )}

            {cancelTarget && (
                <CancelReservationModal
                    reservation={cancelTarget}
                    cancelling={cancelling}
                    error={cancelError}
                    onClose={() => setCancelTarget(null)}
                    onConfirm={handleCancel}
                />
            )}
        </div>
    );
}
