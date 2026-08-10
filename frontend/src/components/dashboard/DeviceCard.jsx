import { useEffect, useState } from "react";
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

export default function DeviceCard({
                                       device,
                                       session,
                                       onStart,
                                       onStop,
                                       onExtend,
                                       onFinishMatch,
                                       onAddMatch,
                                       products,
                                       addingProductId,
                                       onQuickAddProduct,
                                   }) {
    const [now, setNow] = useState(Date.now());

    const active = Boolean(session);

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
            <div className="device-card">
                <div className="device-card-top">
                    <span className="device-type-badge">
                        {device.type}
                    </span>

                    <span className="device-status available">
                        AVAILABLE
                    </span>
                </div>

                <h2 className="device-name">
                    {device.name}
                </h2>

                <div className="device-ready">
                    Ready for next session
                </div>

                <button
                    type="button"
                    className="device-start-button"
                    onClick={() => onStart(device)}
                >
                    Start Session
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
    let timerLabel = "TIME ELAPSED";

    if (isMatch) {
        timerLabel = "MATCH TIME LEFT";

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
        timerLabel = "TIME REMAINING";

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
                    ● PLAYING
                </span>
            </div>

            <div className="device-title-row">
                <h2 className="device-name">
                    {device.name}
                </h2>

                <span className="session-mode-badge">
                    {session.sessionType}
                </span>
            </div>

            {!isMatch &&
                session.plannedMinutes == null && (
                    <div className="open-time-badge">
                        OPEN TIME
                    </div>
                )}

            {matchExpired && (
                <div className="match-expired-banner">
                    MATCH TIME EXPIRED
                </div>
            )}

            {matchEndingSoon && (
                <div className="match-warning-banner">
                    Match ending soon
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
                    <span>Current Cost</span>

                    <strong>
                        {liveAmount.toFixed(2)} EGP
                    </strong>
                </div>

                {!isMatch && (
                    <div>
                        <span>
                            Billing
                        </span>

                        <strong>
                            {session.plannedMinutes == null
                                ? "OPEN"
                                : `${session.plannedMinutes} min`}
                        </strong>
                    </div>
                )}

                {isMatch && (
                    <div>
                        <span>
                            Current Match
                        </span>

                        <strong>
                            {currentMatch}
                            {" / "}
                            {session.purchasedMatches || 1}
                        </strong>
                    </div>
                )}
            </div>

            {isMatch && (
                <div className="match-session-info">
                    <div>
                        <span>Purchased</span>
                        <strong>
                            {session.purchasedMatches || 1}
                        </strong>
                    </div>

                    <div>
                        <span>Completed</span>
                        <strong>
                            {session.completedMatches || 0}
                        </strong>
                    </div>

                    <div>
                        <span>Price / Match</span>
                        <strong>
                            {Number(
                                session.unitPriceSnapshot || 0
                            ).toFixed(2)}
                            {" "}
                            EGP
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
                            +30 min
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
                            +1 hour
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
                            ? "Finish Expired Match"
                            : "Finish Match"}
                    </button>

                    <button
                        type="button"
                        onClick={() =>
                            onAddMatch(session.id)
                        }
                    >
                        + Add Match
                    </button>
                </div>
            )}

            <QuickOrderMenu
                products={products}
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
                onClick={() =>
                    onStop(session.id)
                }
            >
                Stop Session
            </button>
        </div>
    );
}