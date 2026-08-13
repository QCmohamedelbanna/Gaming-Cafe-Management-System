import { useLanguage } from "../../i18n";

export default function PricingCard({
  pricing,
  onChange,
  onSave,
  saving,
}) {
  const { t, formatCurrency, formatNumber } = useLanguage();
  const isMatch = pricing.sessionType === "MATCH";
  const sessionLabel = pricing.sessionType === "SINGLE"
    ? t("modal.single")
    : pricing.sessionType === "MULTI"
      ? t("modal.multi")
      : t("modal.match");

  return (
    <div className="pricing-card">
      <div className="pricing-card-header">
        <div>
          <span className="pricing-badge">
            {pricing.billingUnit === "MATCH" ? t("modal.match") : t("pricing.hour")}
          </span>

          <h3>{sessionLabel}</h3>
        </div>

        <span className={pricing.active ? "pricing-active" : "pricing-inactive"}>
          {pricing.active ? t("pricing.active") : t("pricing.disabled")}
        </span>
      </div>

      <label>{t("pricing.price")}</label>

      <div className="pricing-input-row">
        <input
          type="number"
          min="0.01"
          step="0.5"
          value={pricing.price ?? ""}
          onChange={(e) =>
            onChange(pricing.id, "price", e.target.value)
          }
        />

        <span>
          {t("common.egp")} / {pricing.billingUnit === "MATCH" ? t("modal.match").toLowerCase() : t("pricing.hour").toLowerCase()}
        </span>
      </div>

      {isMatch && (
        <>
          <label>{t("pricing.maximumMatchDuration")}</label>

          <div className="pricing-input-row">
            <input
              type="number"
              min="1"
              value={pricing.matchDurationMinutes ?? ""}
              onChange={(e) =>
                onChange(
                  pricing.id,
                  "matchDurationMinutes",
                  e.target.value
                )
              }
            />

            <span>{t("pricing.minutes")}</span>
          </div>

          <label>{t("pricing.warningBeforeExpiry")}</label>

          <div className="pricing-input-row">
            <input
              type="number"
              min="0"
              value={pricing.warningBeforeExpiryMinutes ?? ""}
              onChange={(e) =>
                onChange(
                  pricing.id,
                  "warningBeforeExpiryMinutes",
                  e.target.value
                )
              }
            />

            <span>{t("pricing.minutes")}</span>
          </div>
        </>
      )}

      <button
        className="pricing-save-button"
        disabled={saving}
        onClick={() => onSave(pricing)}
      >
        {saving ? t("pricing.saving") : t("pricing.saveChanges")}
      </button>
    </div>
  );
}
