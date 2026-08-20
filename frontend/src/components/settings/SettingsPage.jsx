import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";
import { getSettings, updateSettings } from "../../api/settingsApi";

const DISCOUNT_ROLES = ["ADMIN", "MANAGER", "CASHIER"];

function translatedEnum(t, prefix, value) {
    if (!value) return t("common.dash");
    const key = prefix + "." + value;
    const translated = t(key);
    return translated === key ? value : translated;
}

export default function SettingsPage() {
    const { t } = useLanguage();
    const [settings, setSettings] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    useEffect(() => {
        (async () => {
            try {
                setLoading(true);
                setError("");
                setSettings(await getSettings());
            } catch (loadError) {
                console.error(loadError);
                setError(loadError.message || t("settings.loadError"));
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    function updateField(field, value) {
        setSettings((current) => ({ ...current, [field]: value }));
    }

    function toggleDiscountRole(role) {
        setSettings((current) => {
            const has = current.discountAllowedRoles.includes(role);
            return {
                ...current,
                discountAllowedRoles: has
                    ? current.discountAllowedRoles.filter((item) => item !== role)
                    : [...current.discountAllowedRoles, role],
            };
        });
    }

    async function handleSave(event) {
        event.preventDefault();
        try {
            setSaving(true);
            setError("");
            const updated = await updateSettings({
                preventNegativeStock: settings.preventNegativeStock,
                discountAllowedRoles: settings.discountAllowedRoles,
                dashboardEndingSoonMinutes: Number(settings.dashboardEndingSoonMinutes),
                reservationsNoShowGraceMinutes: Number(settings.reservationsNoShowGraceMinutes),
            });
            setSettings(updated);
            setMessage(t("settings.saved"));
        } catch (saveError) {
            console.error(saveError);
            setError(saveError.message || t("settings.saveError"));
        } finally {
            setSaving(false);
        }
    }

    if (loading) {
        return <div>{t("settings.loading")}</div>;
    }

    if (!settings) {
        return <div className="product-error-message">{error}</div>;
    }

    const noDiscountRolesSelected = settings.discountAllowedRoles.length === 0;

    return (
        <div className="products-management-page">
            <div className="products-management-header">
                <div>
                    <span className="page-label">{t("settings.pageLabel")}</span>
                    <h1>{t("settings.title")}</h1>
                    <p>{t("settings.description")}</p>
                </div>
            </div>

            {message && <div className="pricing-message">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}

            <form onSubmit={handleSave} className="settings-form">
                <label className="product-track-toggle">
                    <input
                        type="checkbox"
                        checked={settings.preventNegativeStock}
                        onChange={(event) => updateField("preventNegativeStock", event.target.checked)}
                    />
                    <span>
                        <strong>{t("settings.preventNegativeStock")}</strong>
                        <small>{t("settings.preventNegativeStockHelp")}</small>
                    </span>
                </label>

                <label>{t("settings.discountAllowedRoles")}</label>
                <div className="settings-role-grid">
                    {DISCOUNT_ROLES.map((role) => (
                        <label className="product-track-toggle" key={role}>
                            <input
                                type="checkbox"
                                checked={settings.discountAllowedRoles.includes(role)}
                                onChange={() => toggleDiscountRole(role)}
                            />
                            <span>
                                <strong>{translatedEnum(t, "roles.role", role)}</strong>
                            </span>
                        </label>
                    ))}
                </div>
                {noDiscountRolesSelected && (
                    <div className="checkout-inline-error" role="alert">
                        {t("settings.noDiscountRoles")}
                    </div>
                )}

                <div className="product-form-grid">
                    <div>
                        <label htmlFor="settings-ending-soon">{t("settings.dashboardEndingSoonMinutes")}</label>
                        <input
                            id="settings-ending-soon"
                            type="number"
                            min="1"
                            value={settings.dashboardEndingSoonMinutes}
                            onChange={(event) => updateField("dashboardEndingSoonMinutes", event.target.value)}
                        />
                    </div>
                    <div>
                        <label htmlFor="settings-no-show-grace">{t("settings.reservationsNoShowGraceMinutes")}</label>
                        <input
                            id="settings-no-show-grace"
                            type="number"
                            min="1"
                            value={settings.reservationsNoShowGraceMinutes}
                            onChange={(event) => updateField("reservationsNoShowGraceMinutes", event.target.value)}
                        />
                    </div>
                </div>

                <div className="product-form-actions">
                    <button
                        type="submit"
                        className="primary-action"
                        disabled={saving || noDiscountRolesSelected}
                    >
                        {saving ? t("common.working") : t("common.save")}
                    </button>
                </div>
            </form>
        </div>
    );
}
