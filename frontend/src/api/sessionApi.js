import { apiFetch } from "./http";

const BASE_URL = "/sessions";

async function handleResponse(response) {
    const text = await response.text();
    if (!response.ok) {
        let message = text;
        try {
            const payload = JSON.parse(text);
            message = payload.message || payload.error || text;
        } catch {
            // Keep plain-text API errors.
        }
        throw new Error(message || "Request failed");
    }
    return text ? JSON.parse(text) : null;
}

export async function getActiveSessions() {
    return handleResponse(await apiFetch(`${BASE_URL}/active`, { cache: "no-store" }));
}

export async function startSession({ deviceId, sessionType, plannedMinutes = null, matchCount = null }) {
    return handleResponse(await apiFetch(BASE_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ deviceId, sessionType, plannedMinutes, matchCount }),
    }));
}

export async function stopSession(sessionId) {
    return handleResponse(await apiFetch(`${BASE_URL}/${sessionId}/stop`, { method: "POST" }));
}

export async function prepareCheckout(sessionId) {
    return handleResponse(await apiFetch(`${BASE_URL}/${sessionId}/checkout/prepare`, { method: "POST" }));
}

export async function extendSession(sessionId, minutes) {
    return handleResponse(await apiFetch(`${BASE_URL}/${sessionId}/extend`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ minutes }),
    }));
}

export async function finishMatch(sessionId) {
    return handleResponse(await apiFetch(`${BASE_URL}/${sessionId}/match/finish`, { method: "POST" }));
}

export async function addMatch(sessionId) {
    return handleResponse(await apiFetch(`${BASE_URL}/${sessionId}/match/add`, { method: "POST" }));
}

export async function checkoutSession(sessionId, data) {
    return handleResponse(await apiFetch(`${BASE_URL}/${sessionId}/checkout`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
    }));
}
