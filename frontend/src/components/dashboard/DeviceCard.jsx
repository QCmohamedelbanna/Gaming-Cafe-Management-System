import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";
import QuickOrderMenu from "./QuickOrderMenu";

function formatTime(totalSeconds) {
    const seconds = Math.max(0, totalSeconds);

    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;



    return [hours, minutes, secs]
        .map((value) => String(value).padStart(2, "0"))
        .join(":");
}

function getElapsedSeconds(startTime, now) {
    if (!startTime) return 0;

    const start = new Date(startTime).getTime();

    return Math.max(
        0,
        Math.floor((now - start) / 1000)
    );
}

function PhysicalPower({ device, t }) {
    if (!device.powerControlEnabled) return null;
    const state = device.physicalPowerStatus || "UNKNOWN";
    return (
        <div className={`device-physical-power device-physical-power-${String(state).toLowerCase()}`}>
            {t("device.physicalPower")}: <strong>{state}</strong>
            {device.lastControlError && <small>{device.lastControlError}</small>}
        </div>
    );
}

export default function DeviceCard({
                                       device,
                                       session,
                                       onStart,
                                       order,
                                       onStop,
                                       onExtend,
                                       onFinishMatch,
                                       onAddMatch,
                                       products,
                                       addingProductId,
                                       onQuickAddProduct,
                                       checkoutLoading = false,
}) {
    const { t, formatCurrency, formatNumber } = useLanguage();
    const [now, setNow] = useState(Date.now());

    const active = Boolean(session);
    const deviceEnabled = device.active !== false;
    const deviceAvailable = deviceEnabled && device.status === "AVAILABLE";
    const statusLabel = deviceEnabled
        ? (device.status === "AVAILABLE" ? t("devices.available")
            : device.status === "PLAYING" ? t("devices.playing")
                : device.status === "RESERVED" ? t("devices.reserved")
                    : device.status === "MAINTENANCE" ? t("devices.maintenance")
                        : device.status === "OFFLINE" ? t("devices.offline")
                            : device.status)
        : t("devices.noActive");



    useEffect(() => {
        if (!active) return;

        const interval = setInterval(() => {
            setNow(Date.now());
        }, 1000);

        return () => clearInterval(interval);
    }, [active]);

    /*
     * AVAILABLE DEVICE
     */
    if (!active) {
        return (
            <div className={`device-card device-card-unavailable ${
                deviceAvailable ? "" : "device-card-locked"
            }`}>
                <div className="device-card-top">
                    <span className="device-type-badge">
                        {device.type}
                    </span>

                    <span className={`device-status ${
                        deviceAvailable
                            ? "available"
                            : deviceEnabled
                                ? String(device.status || "OFFLINE").toLowerCase()
                                : "inactive"
                    }`}>
                        {statusLabel}
                    </span>
                </div>

                <PhysicalPower device={device} t={t} />

                <h2 className="device-name">
                    {device.name}
                </h2>

                <div className="device-ready device-availability-message">
                    {deviceAvailable && t("device.ready")}
                    {!deviceEnabled && t("device.deactivated")}
                    {deviceEnabled && device.status === "MAINTENANCE" && (
                        device.maintenanceNote || t("device.maintenanceRequired")
                    )}
                    {deviceEnabled && device.status === "OFFLINE" && t("device.offline")}
                    {deviceEnabled && device.status === "PLAYING" && t("device.refreshing")}
                </div>

                <button
                    type="button"
                    className="device-start-button"
                    disabled={!deviceAvailable}
                    onClick={() => onStart(device)}
                >
                    {deviceAvailable ? t("device.startSession") : t("device.unavailable")}
                </button>
            </div>
        );
    }

    const isMatch =
        session.sessionType === "MATCH";

    const elapsedSeconds =
        getElapsedSeconds(
            session.startTime,
            now
        );

    /*
     * TIMER
     */

    let timerSeconds = elapsedSeconds;
    let timerLabel = t("device.timeElapsed");

    if (isMatch) {
        timerLabel = t("device.matchTimeLeft");

        if (session.currentMatchExpiresAt) {
            timerSeconds = Math.max(
                0,
                Math.floor(
                    (
                        new Date(
                            session.currentMatchExpiresAt
                        ).getTime() - now
                    ) / 1000
                )
            );
        }
    } else if (session.plannedMinutes != null) {
        timerLabel = t("device.timeRemaining");

        timerSeconds = Math.max(
            0,
            session.plannedMinutes * 60 -
            elapsedSeconds
        );
    }

    /*
     * MATCH STATUS
     */

    const matchExpired =
        isMatch &&
        (
            Boolean(session.matchExpired) ||
            timerSeconds <= 0
        );

    const warningSeconds =
        Number(
            session.warningBeforeExpiryMinutesSnapshot
            ?? 2
        ) * 60;

    const matchEndingSoon =
        isMatch &&
        !matchExpired &&
        timerSeconds <= warningSeconds;

    /*
     * COST
     */

    let liveAmount = 0;

    if (isMatch) {
        liveAmount =
            Number(session.unitPriceSnapshot || 0) *
            Number(session.purchasedMatches || 1);
    } else {
        const hourlyRate = Number(
            session.unitPriceSnapshot ||
            session.hourlyRateSnapshot ||
            0
        );

        let billableSeconds =
            elapsedSeconds;

        if (session.plannedMinutes != null) {
            billableSeconds = Math.min(
                elapsedSeconds,
                session.plannedMinutes * 60
            );
        }

        liveAmount =
            hourlyRate *
            (billableSeconds / 3600);
    }

    const orderAmount =
        Number(order?.totalAmount || 0);

    const currentBill =
        liveAmount + orderAmount;

    const currentMatch =
        isMatch
            ? Math.min(
                Number(session.completedMatches || 0) + 1,
                Number(session.purchasedMatches || 1)
            )
            : null;

    /*
     * ACTIVE DEVICE
     */

    return (
        <div
            className={[
                "device-card",
                "device-card-active",
                matchEndingSoon
                    ? "device-match-warning"
                    : "",
                matchExpired
                    ? "device-match-expired"
                    : "",
            ].join(" ")}
        >
            <div className="device-card-top">
                <span className="device-type-badge">
                    {device.type}
                </span>

                <span className="device-status playing">
                    ● {t("device.playing")}
                </span>
            </div>

            <div className="device-title-row">
                <h2 className="device-name">
                    {device.name}
                </h2>

                <span className="session-mode-badge">
                    {session.sessionType === "SINGLE" ? t("modal.single") : session.sessionType === "MULTI" ? t("modal.multi") : t("modal.match")}
                </span>
            </div>

            <PhysicalPower device={device} t={t} />

            {!isMatch &&
                session.plannedMinutes == null && (
                    <div className="open-time-badge">
                        {t("device.openTime")}
                    </div>
                )}

            {matchExpired && (
                <div className="match-expired-banner">
                    {t("device.matchExpired")}
                </div>
            )}

            {matchEndingSoon && (
                <div className="match-warning-banner">
                    {t("device.matchEndingSoon")}
                </div>
            )}

            <div className="session-timer-section">
                <span className="timer-label">
                    {timerLabel}
                </span>

                <strong
                    className={[
                        "session-timer",
                        matchEndingSoon
                            ? "timer-warning"
                            : "",
                        matchExpired
                            ? "timer-expired"
                            : "",
                    ].join(" ")}
                >
                    {formatTime(timerSeconds)}
                </strong>
            </div>

            <div className="session-info-grid">

                <div>
                        <span>{t("device.gamingCost")}</span>

                    <strong>
                        {formatCurrency(liveAmount)}
                    </strong>
                </div>

                <div>
                        <span>{t("device.orders")}</span>

                    <strong>
                        {formatCurrency(orderAmount)}
                    </strong>
                </div>

            </div>

            <div className="current-bill-box">

                <span>{t("device.currentBill")}</span>

                <strong>
                    {formatCurrency(currentBill)}
                </strong>

            </div>

            {isMatch && (
                <div className="match-session-info">
                    <div>
                        <span>{t("device.purchased")}</span>
                        <strong>
                            {session.purchasedMatches || 1}
                        </strong>
                    </div>

                    <div>
                        <span>{t("device.completed")}</span>
                        <strong>
                            {session.completedMatches || 0}
                        </strong>
                    </div>

                    <div>
                        <span>{t("common.pricePerMatch")}</span>
                        <strong>
                            {Number(
                                session.unitPriceSnapshot || 0
                            )}
                        </strong>
                    </div>
                </div>
            )}

            {!isMatch &&
                session.plannedMinutes != null && (
                    <div className="session-actions">
                        <button
                            type="button"
                            onClick={() =>
                                onExtend(
                                    session.id,
                                    30
                                )
                            }
                        >
                            {t("device.extend30")}
                        </button>

                        <button
                            type="button"
                            onClick={() =>
                                onExtend(
                                    session.id,
                                    60
                                )
                            }
                        >
                            {t("device.extendHour")}
                        </button>
                    </div>
                )}

            {isMatch && (
                <div className="session-actions">
                    <button
                        type="button"
                        className={
                            matchExpired
                                ? "match-action-primary"
                                : ""
                        }
                        onClick={() =>
                            onFinishMatch(session.id)
                        }
                    >
                        {matchExpired
                            ? t("device.finishExpiredMatch")
                            : t("device.finishMatch")}
                    </button>

                    <button
                        type="button"
                        onClick={() =>
                            onAddMatch(session.id)
                        }
                    >
                        + {t("device.addMatch")}
                    </button>
                </div>
            )}

            <QuickOrderMenu
                products={products}
                order={order}
                addingProductId={addingProductId}
                onAddProduct={(product) =>
                    onQuickAddProduct(
                        session,
                        product
                    )
                }
            />

            <button
                type="button"
                className="stop-session-button"
                disabled={checkoutLoading}
                onClick={() =>
                    onStop(session, order)
                }
            >
                {checkoutLoading ? t("device.stopping") : t("modal.checkout")}
            </button>
        </div>
    );
}
