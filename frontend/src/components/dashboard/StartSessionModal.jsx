import { useEffect, useMemo, useState } from "react";
import { useLanguage } from "../../i18n";

export default function StartSessionModal({
    device,
    pricing = [],
    onClose,
    onStart,
    loading,
}) {
    const { t, formatCurrency, formatNumber } = useLanguage();
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
                        <span className="page-label">{t("modal.startSession")}</span>

                        <h2 id="start-session-title">{device.name}</h2>

                        <p>{device.type}</p>
                    </div>

                    <button
                        type="button"
                        className="modal-close"
                        aria-label={t("modal.closeStartSession")}
                        disabled={loading}
                        onClick={onClose}
                    >
                        &times;
                    </button>
                </div>

                {!sessionType && (
                    <>
                        <h3>{t("modal.chooseSessionType")}</h3>

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
                                        <strong>{type === "SINGLE" ? t("modal.single") : type === "MULTI" ? t("modal.multi") : t("modal.match")}</strong>

                                        <span>
                                            {formatCurrency(rule.price)} / {" "}
                                            {rule.billingUnit === "MATCH"
                                                ? t("modal.match")
                                                : t("pricing.hour")}
                                        </span>

                                        {type === "MATCH" && (
                                            <small>
                                                {formatNumber(rule.matchDurationMinutes)} {t("pricing.minutes")} {t("modal.maximumDuration").toLowerCase()}
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
                            &larr; {t("modal.changeSessionType")}
                        </button>

                        <div className="selected-session">
                            <strong>{sessionType === "SINGLE" ? t("modal.single") : t("modal.multi")}</strong>

                            <span>{formatCurrency(selectedRule?.price)} / {t("pricing.hour").toLowerCase()}</span>
                        </div>

                        <h3>{t("modal.selectDuration")}</h3>

                        <div className="duration-grid">
                            <button
                                type="button"
                                className={duration === 30
                                    ? "duration-button selected"
                                    : "duration-button"}
                                aria-pressed={duration === 30}
                                onClick={() => setDuration(30)}
                            >
                                {t("modal.minutes30")}
                            </button>

                            <button
                                type="button"
                                className={duration === 60
                                    ? "duration-button selected"
                                    : "duration-button"}
                                aria-pressed={duration === 60}
                                onClick={() => setDuration(60)}
                            >
                                {t("modal.hour1")}
                            </button>

                            <button
                                type="button"
                                className={duration === null
                                    ? "duration-button selected"
                                    : "duration-button"}
                                aria-pressed={duration === null}
                                onClick={() => setDuration(null)}
                            >
                                {t("modal.openTime")}
                            </button>
                        </div>

                        <button
                            type="button"
                            className="primary-action"
                            disabled={loading || duration === undefined}
                            onClick={start}
                        >
                            {loading ? t("modal.starting") : t("modal.startSession")}
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
                            &larr; {t("modal.changeSessionType")}
                        </button>

                        <div className="selected-session">
                            <strong>{t("modal.match")}</strong>

                            <span>{formatCurrency(selectedRule?.price)} / {t("modal.match").toLowerCase()}</span>
                        </div>

                        <div className="match-config">
                            <div>
                                <span>{t("modal.maximumDuration")}</span>
                                <strong>
                                    {formatNumber(selectedRule?.matchDurationMinutes)} {t("pricing.minutes")}
                                </strong>
                            </div>

                            <div>
                                <span>{t("modal.warning")}</span>
                                <strong>
                                    {formatNumber(selectedRule?.warningBeforeExpiryMinutes)} {t("modal.beforeExpiry")}
                                </strong>
                            </div>
                        </div>

                        <label htmlFor="match-count">{t("modal.matchCount")}</label>

                        <div className="match-counter" id="match-count">
                            <button
                                type="button"
                                aria-label={t("modal.decreaseMatches")}
                                onClick={() =>
                                    setMatchCount((value) => Math.max(1, value - 1))
                                }
                            >
                                &minus;
                            </button>

                            <strong>{matchCount}</strong>

                            <button
                                type="button"
                                aria-label={t("modal.increaseMatches")}
                                onClick={() => setMatchCount((value) => value + 1)}
                            >
                                +
                            </button>
                        </div>

                        <div className="match-total">
                            <span>{t("modal.total")}</span>

                            <strong>
                                {formatCurrency(Number(selectedRule?.price || 0) * matchCount)}
                            </strong>
                        </div>

                        <button
                            type="button"
                            className="primary-action"
                            disabled={loading}
                            onClick={start}
                        >
                            {loading ? t("modal.starting") : t("modal.startMatch")}
                        </button>
                    </>
                )}
            </div>
        </div>
    );
}
