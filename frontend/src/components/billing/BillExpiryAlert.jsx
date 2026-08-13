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

function ring(contextRef) {
    const context = getAudioContext(contextRef);
    if (!context) return;

    const play = () => {
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
        oscillator.start(startAt);
        oscillator.stop(startAt + 0.42);
    };

    if (context.state === "running") {
        play();
    } else {
        context.resume().then(play).catch(() => {});
    }
}

export default function BillExpiryAlert({ onNavigate }) {
    const { t, formatCurrency, formatNumber, language } = useLanguage();
    const [alerts, setAlerts] = useState([]);
    const audioContextRef = useRef(null);
    const alertSignatureRef = useRef("");

    const loadAlerts = useCallback(async () => {
        try {
            const nextAlerts = await getBillAlerts();
            const signature = nextAlerts
                .map((alert) => `${alert.billId}:${alert.notificationExpiresAt}`)
                .join("|");

            if (signature !== alertSignatureRef.current) {
                alertSignatureRef.current = signature;
                setAlerts(nextAlerts);
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
        if (alerts.length === 0) return undefined;

        ring(audioContextRef);
        const repeat = window.setInterval(
            () => ring(audioContextRef),
            RING_REPEAT_MS
        );

        return () => window.clearInterval(repeat);
    }, [alerts]);

    if (alerts.length === 0) return null;

    return (
        <aside className="bill-expiry-alert" role="alert" aria-live="assertive">
            <div className="bill-expiry-alert-heading">
                <span className="bill-expiry-alert-dot" />
                <strong>{t("common.sessionEnded")}</strong>
                <span>{t("billing.billCount", { count: formatNumber(alerts.length), suffix: language === "ar" ? "" : alerts.length === 1 ? "" : "s" })}</span>
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
                onClick={() => onNavigate("billing")}
            >
                {t("billing.openBilling")}
            </button>
        </aside>
    );
}
