import { useEffect, useMemo, useState } from "react";

export default function StartSessionModal({
    device,
    pricing = [],
    onClose,
    onStart,
    loading,
}) {
    const [sessionType, setSessionType] = useState(null);
    const [duration, setDuration] = useState(undefined);
    const [matchCount, setMatchCount] = useState(1);

    const rules = useMemo(
        () => pricing.filter((rule) => rule.deviceType === device.type),
        [pricing, device.type]
    );

    function ruleFor(type) {
        return rules.find((rule) => rule.sessionType === type);
    }

    const selectedRule = sessionType ? ruleFor(sessionType) : null;

    useEffect(() => {
        function handleKeyDown(event) {
            if (event.key === "Escape" && !loading) {
                onClose();
            }
        }

        window.addEventListener("keydown", handleKeyDown);

        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [loading, onClose]);

    function start() {
        if (!sessionType) return;

        if (sessionType === "MATCH") {
            onStart({
                deviceId: device.id,
                sessionType,
                plannedMinutes: null,
                matchCount,
            });

            return;
        }

        onStart({
            deviceId: device.id,
            sessionType,
            plannedMinutes: duration,
            matchCount: null,
        });
    }

    function handleOverlayMouseDown(event) {
        if (event.target === event.currentTarget && !loading) {
            onClose();
        }
    }

    return (
        <div
            className="modal-overlay"
            role="presentation"
            onMouseDown={handleOverlayMouseDown}
        >
            <div
                className="modal-container session-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="start-session-title"
            >
                <div className="modal-header">
                    <div>
                        <span className="page-label">START SESSION</span>

                        <h2 id="start-session-title">{device.name}</h2>

                        <p>{device.type}</p>
                    </div>

                    <button
                        type="button"
                        className="modal-close"
                        aria-label="Close start session"
                        disabled={loading}
                        onClick={onClose}
                    >
                        &times;
                    </button>
                </div>

                {!sessionType && (
                    <>
                        <h3>Choose session type</h3>

                        <div className="session-type-grid">
                            {["SINGLE", "MULTI", "MATCH"].map((type) => {
                                const rule = ruleFor(type);

                                if (!rule || !rule.active) {
                                    return null;
                                }

                                return (
                                    <button
                                        type="button"
                                        key={type}
                                        className="session-type-card"
                                        onClick={() => setSessionType(type)}
                                    >
                                        <strong>{type}</strong>

                                        <span>
                                            {rule.price} EGP / {" "}
                                            {rule.billingUnit === "MATCH"
                                                ? "match"
                                                : "hour"}
                                        </span>

                                        {type === "MATCH" && (
                                            <small>
                                                {rule.matchDurationMinutes} min maximum
                                            </small>
                                        )}
                                    </button>
                                );
                            })}
                        </div>
                    </>
                )}

                {sessionType && sessionType !== "MATCH" && (
                    <>
                        <button
                            type="button"
                            className="back-button"
                            onClick={() => {
                                setSessionType(null);
                                setDuration(undefined);
                            }}
                        >
                            &larr; Change session type
                        </button>

                        <div className="selected-session">
                            <strong>{sessionType}</strong>

                            <span>{selectedRule?.price} EGP / hour</span>
                        </div>

                        <h3>Choose duration</h3>

                        <div className="duration-grid">
                            <button
                                type="button"
                                className={duration === 30
                                    ? "duration-button selected"
                                    : "duration-button"}
                                aria-pressed={duration === 30}
                                onClick={() => setDuration(30)}
                            >
                                30 Minutes
                            </button>

                            <button
                                type="button"
                                className={duration === 60
                                    ? "duration-button selected"
                                    : "duration-button"}
                                aria-pressed={duration === 60}
                                onClick={() => setDuration(60)}
                            >
                                1 Hour
                            </button>

                            <button
                                type="button"
                                className={duration === null
                                    ? "duration-button selected"
                                    : "duration-button"}
                                aria-pressed={duration === null}
                                onClick={() => setDuration(null)}
                            >
                                Open Time
                            </button>
                        </div>

                        <button
                            type="button"
                            className="primary-action"
                            disabled={loading || duration === undefined}
                            onClick={start}
                        >
                            {loading ? "Starting..." : "Start Session"}
                        </button>
                    </>
                )}

                {sessionType === "MATCH" && (
                    <>
                        <button
                            type="button"
                            className="back-button"
                            onClick={() => setSessionType(null)}
                        >
                            &larr; Change session type
                        </button>

                        <div className="selected-session">
                            <strong>MATCH</strong>

                            <span>{selectedRule?.price} EGP / match</span>
                        </div>

                        <div className="match-config">
                            <div>
                                <span>Maximum duration</span>
                                <strong>
                                    {selectedRule?.matchDurationMinutes} min
                                </strong>
                            </div>

                            <div>
                                <span>Warning</span>
                                <strong>
                                    {selectedRule?.warningBeforeExpiryMinutes} min before expiry
                                </strong>
                            </div>
                        </div>

                        <label htmlFor="match-count">Number of matches</label>

                        <div className="match-counter" id="match-count">
                            <button
                                type="button"
                                aria-label="Decrease number of matches"
                                onClick={() =>
                                    setMatchCount((value) => Math.max(1, value - 1))
                                }
                            >
                                &minus;
                            </button>

                            <strong>{matchCount}</strong>

                            <button
                                type="button"
                                aria-label="Increase number of matches"
                                onClick={() => setMatchCount((value) => value + 1)}
                            >
                                +
                            </button>
                        </div>

                        <div className="match-total">
                            <span>Total</span>

                            <strong>
                                {(Number(selectedRule?.price || 0) * matchCount).toFixed(2)} EGP
                            </strong>
                        </div>

                        <button
                            type="button"
                            className="primary-action"
                            disabled={loading}
                            onClick={start}
                        >
                            {loading ? "Starting..." : "Start Match"}
                        </button>
                    </>
                )}
            </div>
        </div>
    );
}
