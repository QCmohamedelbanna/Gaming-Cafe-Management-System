import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";
import {
  getPricing,
  updatePricing,
} from "../../api/pricingApi";

import PricingCard from "./PricingCard";

export default function PricingPage() {
  const { t } = useLanguage();
  const [pricing, setPricing] = useState([]);
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState(null);
  const [message, setMessage] = useState("");

  async function loadPricing() {
    try {
      setLoading(true);
      const data = await getPricing();
      setPricing(data);
    } catch (error) {
      console.error(error);
      setMessage(t("pricing.loadError"));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadPricing();
  }, []);

  function handleChange(id, field, value) {
    setPricing((current) =>
      current.map((rule) =>
        rule.id === id
          ? { ...rule, [field]: value }
          : rule
      )
    );
  }

  async function handleSave(rule) {
    try {
      setSavingId(rule.id);
      setMessage("");

      const body = {
        price: Number(rule.price),
        matchDurationMinutes:
          rule.sessionType === "MATCH"
            ? Number(rule.matchDurationMinutes)
            : null,
        warningBeforeExpiryMinutes:
          rule.sessionType === "MATCH"
            ? Number(rule.warningBeforeExpiryMinutes)
            : null,
      };

      const updated = await updatePricing(rule.id, body);

      setPricing((current) =>
        current.map((item) =>
          item.id === updated.id ? updated : item
        )
      );

      setMessage(t("pricing.saved", { name: `${updated.deviceType} ${updated.sessionType}` }));
    } catch (error) {
      console.error(error);
      setMessage(error.message || t("pricing.saveError"));
    } finally {
      setSavingId(null);
    }
  }

  if (loading) {
    return <div>{t("pricing.loading")}</div>;
  }

  return (
    <div className="pricing-page">
      <div className="pricing-header">
        <div>
          <span className="page-label">{t("pricing.pageLabel")}</span>
          <h1>{t("pricing.title")}</h1>
          <p>{t("pricing.descriptionShort")}</p>
        </div>

        <button
          className="refresh-button"
          onClick={loadPricing}
        >
          {t("common.refresh")}
        </button>
      </div>

      {message && (
        <div className="pricing-message">
          {message}
        </div>
      )}

      {["PS4", "PS5"].map((deviceType) => (
        <section
          className="pricing-device-section"
          key={deviceType}
        >
          <h2>{deviceType}</h2>

          <div className="pricing-grid">
            {pricing
              .filter(
                (rule) =>
                  rule.deviceType === deviceType
              )
              .map((rule) => (
                <PricingCard
                  key={rule.id}
                  pricing={rule}
                  onChange={handleChange}
                  onSave={handleSave}
                  saving={savingId === rule.id}
                />
              ))}
          </div>
        </section>
      ))}
    </div>
  );
}
