import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";
import { getUpcomingReservations } from "../../api/reservationApi";

export default function UpcomingReservationsPanel() {
    const { t, language } = useLanguage();
    const [reservations, setReservations] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let cancelled = false;

        getUpcomingReservations()
            .then((data) => {
                if (!cancelled) setReservations(data);
            })
            .catch((error) => {
                console.error("Could not load upcoming reservations", error);
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, []);

    if (loading || reservations.length === 0) return null;

    return (
        <div className="upcoming-reservations-panel">
            <span className="page-label">{t("dashboard.upcomingReservations")}</span>
            <div className="upcoming-reservations-list">
                {reservations.slice(0, 5).map((reservation) => (
                    <div className="upcoming-reservation-row" key={reservation.id}>
                        <strong>
                            {new Date(reservation.startTime).toLocaleTimeString(
                                language === "ar" ? "ar-EG" : "en-EG",
                                { hour: "2-digit", minute: "2-digit" }
                            )}
                        </strong>
                        <span>{reservation.device.name}</span>
                        <span>{reservation.customer.name}</span>
                    </div>
                ))}
            </div>
        </div>
    );
}
