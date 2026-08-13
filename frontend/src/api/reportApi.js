const BASE_URL = "http://localhost:8080/api/reports";

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
        throw new Error(message || "Report request failed");
    }

    return text ? JSON.parse(text) : null;
}

export async function getReport(from, to) {
    const params = new URLSearchParams();
    if (from) params.set("from", from);
    if (to) params.set("to", to);

    const query = params.toString();
    return handleResponse(await fetch(`${BASE_URL}${query ? `?${query}` : ""}`));
}
