const BASE_URL = "http://localhost:8080/api/sessions";
const CURRENT_CASHIER = "Admin";

async function handleResponse(response) {
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || "Request failed");
  }

  return response.json();
}

export async function getActiveSessions() {
  const response = await fetch(`${BASE_URL}/active`);
  return handleResponse(response);
}

export async function startSession({
                                     deviceId,
                                     sessionType,
                                     plannedMinutes = null,
                                     matchCount = null,
                                   }) {
  const response = await fetch(BASE_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      deviceId,
      sessionType,
      plannedMinutes,
      matchCount,
    }),
  });

  return handleResponse(response);
}

export async function stopSession(sessionId) {
    const response = await fetch(`${BASE_URL}/${sessionId}/stop`, {
        method: "POST",
    });

    return handleResponse(response);
}

export async function prepareCheckout(sessionId) {
    const response = await fetch(
        `${BASE_URL}/${sessionId}/checkout/prepare`,
        {
            method: "POST",
        }
    );

    return handleResponse(response);
}

export async function extendSession(sessionId, minutes) {
  const response = await fetch(`${BASE_URL}/${sessionId}/extend`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      minutes,
    }),
  });

  return handleResponse(response);
}

export async function finishMatch(sessionId) {
  const response = await fetch(
      `${BASE_URL}/${sessionId}/match/finish`,
      {
        method: "POST",
      }
  );

  return handleResponse(response);
}

export async function addMatch(sessionId) {
  const response = await fetch(
      `${BASE_URL}/${sessionId}/match/add`,
      {
        method: "POST",
      }
  );

  return handleResponse(response);
}

export async function checkoutSession(sessionId, data) {
  const response = await fetch(
      `${BASE_URL}/${sessionId}/checkout`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Cashier": CURRENT_CASHIER,
        },
        body: JSON.stringify(data),
      }
  );

  return handleResponse(response);
}
