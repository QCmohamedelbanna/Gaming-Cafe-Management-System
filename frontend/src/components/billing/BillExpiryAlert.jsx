import { useCallback, useEffect, useRef, useState } from "react";
import { useLanguage } from "../../i18n";
import { getBillAlerts } from "../../api/billingApi";

const ALERT_POLL_MS = 2000;
const RING_REPEAT_MS = 4000;

function money(value) {
    return `${Number(value || 0).toFixed(2)} EGP`;
}

function getAudioContext(contextRef) {
    if (contextRef.current) return contextRef.current;

    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (!AudioContext) return null;

    try {
        contextRef.current = new AudioContext();
        return contextRef.current;
    } catch {
        return null;
    }
}

function ring(contextRef, activeOscillatorsRef, ringingRef) {
    const context = getAudioContext(contextRef);
    if (!context || !ringingRef.current) return;

    const play = () => {
        if (!ringingRef.current) return;

        const oscillator = context.createOscillator();
        const gain = context.createGain();
        const startAt = context.currentTime;

        oscillator.type = "sine";
        oscillator.frequency.setValueAtTime(880, startAt);
        oscillator.frequency.setValueAtTime(660, startAt + 0.18);

        gain.gain.setValueAtTime(0.0001, startAt);
        gain.gain.exponentialRampToValueAtTime(0.18, startAt + 0.03);
        gain.gain.exponentialRampToValueAtTime(0.0001, startAt + 0.42);

        oscillator.connect(gain);
        gain.connect(context.destination);
        activeOscillatorsRef.current.add(oscillator);
        oscillator.addEventListener("ended", () => {
            activeOscillatorsRef.current.delete(oscillator);
        }, { once: true });
        oscillator.start(startAt);
        oscillator.stop(startAt + 0.42);
    };

    if (context.state === "running") {
        play();
    } else {
        context.resume().then(() => {
            if (ringingRef.current) play();
        }).catch(() => {});
    }
}

function stopRinging(contextRef, activeOscillatorsRef) {
    activeOscillatorsRef.current.forEach((oscillator) => {
        try {
            oscillator.stop();
        } catch {
            // The oscillator may already have completed its short beep.
        }
        try {
            oscillator.disconnect();
        } catch {
            // Ignore nodes that have already been disconnected.
        }
    });
    activeOscillatorsRef.current.clear();

    const context = contextRef.current;
    if (context?.state === "running") {
        context.suspend().catch(() => {});
    }
}

export default function BillExpiryAlert({ onNavigate }) {
    const { t, formatCurrency, formatNumber, language } = useLanguage();
    const [alerts, setAlerts] = useState([]);
    const audioContextRef = useRef(null);
    const alertSignatureRef = useRef("");
    const dismissedAlertIdsRef = useRef(new Set());
    const activeOscillatorsRef = useRef(new Set());
    const ringingRef = useRef(false);

    const loadAlerts = useCallback(async () => {
        try {
            const nextAlerts = await getBillAlerts();
            const activeAlertIds = new Set(nextAlerts.map((alert) => alert.billId));
            dismissedAlertIdsRef.current.forEach((billId) => {
                if (!activeAlertIds.has(billId)) {
                    dismissedAlertIdsRef.current.delete(billId);
                }
            });

            const visibleAlerts = nextAlerts.filter(
                (alert) => !dismissedAlertIdsRef.current.has(alert.billId)
            );
            const signature = visibleAlerts
                .map((alert) => `${alert.billId}:${alert.notificationExpiresAt}`)
                .join("|");

            if (signature !== alertSignatureRef.current) {
                alertSignatureRef.current = signature;
                setAlerts(visibleAlerts);
            }
        } catch {
            // A temporary polling failure should not interrupt the cashier UI.
        }
    }, []);

    useEffect(() => {
        loadAlerts();
        const poll = window.setInterval(loadAlerts, ALERT_POLL_MS);
        return () => window.clearInterval(poll);
    }, [loadAlerts]);

    useEffect(() => {
        function primeAudio() {
            const context = getAudioContext(audioContextRef);
            if (context?.state === "suspended") {
                context.resume().catch(() => {});
            }
        }

        window.addEventListener("pointerdown", primeAudio);
        return () => window.removeEventListener("pointerdown", primeAudio);
    }, []);

    useEffect(() => {
        if (alerts.length === 0) {
            ringingRef.current = false;
            stopRinging(audioContextRef, activeOscillatorsRef);
            return undefined;
        }

        ringingRef.current = true;
        ring(audioContextRef, activeOscillatorsRef, ringingRef);
        const repeat = window.setInterval(
            () => ring(audioContextRef, activeOscillatorsRef, ringingRef),
            RING_REPEAT_MS
        );

        return () => {
            window.clearInterval(repeat);
            ringingRef.current = false;
            stopRinging(audioContextRef, activeOscillatorsRef);
        };
    }, [alerts]);

    function dismissAlerts() {
        alerts.forEach((alert) => dismissedAlertIdsRef.current.add(alert.billId));
        alertSignatureRef.current = "";
        ringingRef.current = false;
        stopRinging(audioContextRef, activeOscillatorsRef);
        setAlerts([]);
    }

    function openBilling() {
        dismissAlerts();
        onNavigate("billing");
    }

    if (alerts.length === 0) return null;

    return (
        <aside className="bill-expiry-alert" role="alert" aria-live="assertive">
            <div className="bill-expiry-alert-heading">
                <span className="bill-expiry-alert-dot" />
                <strong>{t("common.sessionEnded")}</strong>
                <span className="bill-expiry-alert-count">{t("billing.billCount", { count: formatNumber(alerts.length), suffix: language === "ar" ? "" : alerts.length === 1 ? "" : "s" })}</span>
                <button
                    type="button"
                    className="bill-expiry-alert-close"
                    onClick={dismissAlerts}
                    aria-label={t("common.close")}
                >
                    {t("common.close")}
                </button>
            </div>

            <div className="bill-expiry-alert-list">
                {alerts.map((alert) => (
                    <div className="bill-expiry-alert-item" key={alert.billId}>
                        <div>
                            <strong>
                                {alert.deviceName ||
                                    t("billing.referenceSession", { id: alert.sessionId })}
                            </strong>
                            <span>
                                {alert.billNumber} · {formatCurrency(alert.totalAmount)}
                            </span>
                        </div>
                    </div>
                ))}
            </div>

            <button
                type="button"
                className="bill-expiry-alert-action"
                onClick={openBilling}
            >
                {t("billing.openBilling")}
            </button>
        </aside>
    );
}
