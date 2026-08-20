import { apiFetch } from "./http";

const BASE_URL = "/inventory";

async function handleResponse(response) {
    const text = await response.text();
    let payload = null;

    if (text) {
        try {
            payload = JSON.parse(text);
        } catch {
            payload = text;
        }
    }

    if (!response.ok) {
        throw new Error(
            typeof payload === "object" && payload?.message
                ? payload.message
                : typeof payload === "string" && payload
                    ? payload
                    : "Inventory request failed"
        );
    }

    return payload;
}

export async function getInventoryProducts() {
    return handleResponse(await apiFetch(`${BASE_URL}/products`));
}

export async function getInventoryCategories() {
    return handleResponse(await apiFetch(`${BASE_URL}/categories`));
}

export async function getStockMovements(productId = null) {
    const query = productId == null ? "" : `?productId=${productId}`;
    return handleResponse(await apiFetch(`${BASE_URL}/movements${query}`));
}

export async function purchaseStock(productId, data) {
    return handleResponse(await apiFetch(
        `${BASE_URL}/products/${productId}/purchase`,
        {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data),
        }
    ));
}

export async function adjustStock(productId, data) {
    return handleResponse(await apiFetch(
        `${BASE_URL}/products/${productId}/adjust`,
        {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data),
        }
    ));
}

export async function recordWaste(productId, data) {
    return handleResponse(await apiFetch(
        `${BASE_URL}/products/${productId}/waste`,
        {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data),
        }
    ));
}
