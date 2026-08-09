import { useMemo, useState } from "react";

export default function StartSessionModal({
  device,
  pricing,
  onClose,
  onStart,
  loading,
}) {
  const [sessionType, setSessionType] = useState(null);


  const [matchCount, setMatchCount] = useState(1);

  const rules = useMemo(
    () =>
      pricing.filter(
        (rule) => rule.deviceType === device.type
      ),
    [pricing, device]
  );

  function ruleFor(type) {
    return rules.find(
      (rule) => rule.sessionType === type
    );
  }

  const selectedRule = sessionType
    ? ruleFor(sessionType)
    : null;

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

  return (
    <div className="modal-overlay">
      <div className="session-modal">
        <div className="session-modal-header">
          <div>
            <span className="page-label">
              START SESSION
            </span>

            <h2>{device.name}</h2>

            <p>{device.type}</p>
          </div>

          <button
            className="modal-close"
            onClick={onClose}
          >
            ×
          </button>
        </div>

        {!sessionType && (
          <>
            <h3>Choose session type</h3>

            <div className="session-type-grid">
              {["SINGLE", "MULTI", "MATCH"].map(
                (type) => {
                  const rule = ruleFor(type);

                  if (!rule || !rule.active) {
                    return null;
                  }

                  return (
                    <button
                      key={type}
                      className="session-type-card"
                      onClick={() =>
                        setSessionType(type)
                      }
                    >
                      <strong>{type}</strong>

                      <span>
                        {rule.price} EGP /{" "}
                        {rule.billingUnit === "MATCH"
                          ? "match"
                          : "hour"}
                      </span>

                      {type === "MATCH" && (
                        <small>
                          {
                            rule.matchDurationMinutes
                          }{" "}
                          min maximum
                        </small>
                      )}
                    </button>
                  );
                }
              )}
            </div>
          </>
        )}

        {sessionType &&
          sessionType !== "MATCH" && (
            <>
              <button
                className="back-button"
                onClick={() => {
                  setSessionType(null);
                  setDuration(null);
                }}
              >
                ← Change session type
              </button>

              <div className="selected-session">
                <strong>{sessionType}</strong>

                <span>
                  {selectedRule?.price} EGP /
                  hour
                </span>
              </div>

              <h3>Choose duration</h3>

              <div className="duration-grid">
                <button
                  className={
                    duration === 30
                      ? "duration-button selected"
                      : "duration-button"
                  }
                  onClick={() => setDuration(30)}
                >
                  30 Minutes
                </button>

                <button
                  className={
                    duration === 60
                      ? "duration-button selected"
                      : "duration-button"
                  }
                  onClick={() => setDuration(60)}
                >
                  1 Hour
                </button>

                <button
                  className={
                    duration === null
                      ? "duration-button selected"
                      : "duration-button"
                  }
                  onClick={() => setDuration(null)}
                >
                  Open Time
                </button>
              </div>

              <button
                className="primary-action"
                disabled={loading}
                onClick={start}
              >
                {loading
                  ? "Starting..."
                  : "Start Session"}
              </button>
            </>
          )}

        {sessionType === "MATCH" && (
          <>
            <button
              className="back-button"
              onClick={() =>
                setSessionType(null)
              }
            >
              ← Change session type
            </button>

            <div className="selected-session">
              <strong>MATCH</strong>

              <span>
                {selectedRule?.price} EGP /
                match
              </span>
            </div>

            <div className="match-config">
              <div>
                <span>Maximum duration</span>
                <strong>
                  {
                    selectedRule?.matchDurationMinutes
                  }{" "}
                  min
                </strong>
              </div>

              <div>
                <span>Warning</span>
                <strong>
                  {
                    selectedRule?.warningBeforeExpiryMinutes
                  }{" "}
                  min before expiry
                </strong>
              </div>
            </div>

            <label>Number of matches</label>

            <div className="match-counter">
              <button
                onClick={() =>
                  setMatchCount((value) =>
                    Math.max(1, value - 1)
                  )
                }
              >
                −
              </button>

              <strong>{matchCount}</strong>

              <button
                onClick={() =>
                  setMatchCount(
                    (value) => value + 1
                  )
                }
              >
                +
              </button>
            </div>

            <div className="match-total">
              <span>Total</span>

              <strong>
                {(
                  Number(
                    selectedRule?.price || 0
                  ) * matchCount
                ).toFixed(2)}{" "}
                EGP
              </strong>
            </div>

            <button
              className="primary-action"
              disabled={loading}
              onClick={start}
            >
              {loading
                ? "Starting..."
                : "Start Match"}
            </button>
          </>
        )}
      </div>
    </div>
  );
}