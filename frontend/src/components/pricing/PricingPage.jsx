import { useEffect, useState } from "react";
import {
  getPricing,
  updatePricing,
} from "../../api/pricingApi";

import PricingCard from "./PricingCard";

export default function PricingPage() {
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
      setMessage("Could not load pricing.");
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

      setMessage(
        `${updated.deviceType} ${updated.sessionType} saved successfully`
      );
    } catch (error) {
      console.error(error);
      setMessage(error.message || "Failed to save pricing");
    } finally {
      setSavingId(null);
    }
  }

  if (loading) {
    return <div>Loading pricing...</div>;
  }

  return (
    <div className="pricing-page">
      <div className="pricing-header">
        <div>
          <span className="page-label">SETTINGS</span>
          <h1>Pricing</h1>
          <p>Manage gaming prices and match limits.</p>
        </div>

        <button
          className="refresh-button"
          onClick={loadPricing}
        >
          Refresh
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