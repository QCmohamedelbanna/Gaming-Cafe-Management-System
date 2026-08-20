import { apiFetch } from "./http";

const BASE_URL = "/orders";

async function handleResponse(response) {
    const text = await response.text();

    if (!response.ok) {
        let message = text;
        try {
            const payload = JSON.parse(text);
            message = payload.message || payload.error || text;
        } catch {
            // Keep plain-text API errors as-is.
        }
        throw new Error(message || "Request failed");
    }

    return text ? JSON.parse(text) : null;
}

export async function createOrder(gameSessionId = null) {
    return handleResponse(await apiFetch(BASE_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ gameSessionId }),
    }));
}

export async function addOrderItem(orderId, productId, quantity = 1) {
    return handleResponse(await apiFetch(`${BASE_URL}/${orderId}/items`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ productId, quantity }),
    }));
}

export async function removeOrderItem(orderId, itemId) {
    return handleResponse(await apiFetch(`${BASE_URL}/${orderId}/items/${itemId}`, {
        method: "DELETE",
    }));
}

export async function updateOrderItemQuantity(orderId, itemId, quantity) {
    return handleResponse(await apiFetch(`${BASE_URL}/${orderId}/items/${itemId}/quantity`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ quantity }),
    }));
}

export async function applyOrderDiscount(orderId, data) {
    return handleResponse(await apiFetch(`${BASE_URL}/${orderId}/discount`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
    }));
}

export async function clearOrderDiscount(orderId) {
    return handleResponse(await apiFetch(`${BASE_URL}/${orderId}/discount`, {
        method: "DELETE",
    }));
}

export async function holdOrder(orderId) {
    return handleResponse(await apiFetch(`${BASE_URL}/${orderId}/hold`, { method: "POST" }));
}

export async function resumeOrder(orderId) {
    return handleResponse(await apiFetch(`${BASE_URL}/${orderId}/resume`, { method: "POST" }));
}

export async function cancelOrder(orderId) {
    return handleResponse(await apiFetch(`${BASE_URL}/${orderId}/cancel`, { method: "POST" }));
}

export async function getHeldOrders() {
    return handleResponse(await apiFetch(`${BASE_URL}/held`));
}

export async function completeOrder(orderId, payment) {
    return handleResponse(await apiFetch(`${BASE_URL}/${orderId}/complete`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payment),
    }));
}

export async function getOpenOrderForSession(sessionId) {
    const response = await apiFetch(`${BASE_URL}/session/${sessionId}/open`, { cache: "no-store" });
    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || "Failed to load session order");
    }
    const text = await response.text();
    return text ? JSON.parse(text) : null;
}
