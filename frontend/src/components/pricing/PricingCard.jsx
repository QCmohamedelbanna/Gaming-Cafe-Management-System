export default function PricingCard({
  pricing,
  onChange,
  onSave,
  saving,
}) {
  const isMatch = pricing.sessionType === "MATCH";

  return (
    <div className="pricing-card">
      <div className="pricing-card-header">
        <div>
          <span className="pricing-badge">
            {pricing.billingUnit}
          </span>

          <h3>{pricing.sessionType}</h3>
        </div>

        <span className={pricing.active ? "pricing-active" : "pricing-inactive"}>
          {pricing.active ? "ACTIVE" : "DISABLED"}
        </span>
      </div>

      <label>Price</label>

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
          EGP / {pricing.billingUnit === "MATCH" ? "match" : "hour"}
        </span>
      </div>

      {isMatch && (
        <>
          <label>Maximum Match Duration</label>

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

            <span>minutes</span>
          </div>

          <label>Warning Before Expiry</label>

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

            <span>minutes</span>
          </div>
        </>
      )}

      <button
        className="pricing-save-button"
        disabled={saving}
        onClick={() => onSave(pricing)}
      >
        {saving ? "Saving..." : "Save Changes"}
      </button>
    </div>
  );
}