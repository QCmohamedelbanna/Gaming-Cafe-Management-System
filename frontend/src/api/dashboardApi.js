import { apiFetch } from "./http";

const BASE_URL = "http://localhost:8080/api/dashboard";

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
        throw new Error(message || "Dashboard request failed");
    }

    return text ? JSON.parse(text) : null;
}

export async function getDashboardSummary() {
    return handleResponse(await apiFetch(`${BASE_URL}/today`));
}
