import { apiFetch } from "./http";

const BASE_URL = "http://localhost:8080/api/devices";

async function handleResponse(response) {
    const text = await response.text();

    if (!response.ok) {
        let message = text;

        try {
            message = JSON.parse(text).message || text;
        } catch {
            // Keep plain-text API errors as-is.
        }

        throw new Error(message || "Device request failed");
    }

    return text ? JSON.parse(text) : null;
}

export async function getDevices() {
    return handleResponse(await apiFetch(BASE_URL, { cache: "no-store" }));
}

export async function createDevice(data) {
    return handleResponse(await apiFetch(BASE_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
    }));
}

export async function updateDevice(id, data) {
    return handleResponse(await apiFetch(`${BASE_URL}/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
    }));
}

export async function setDeviceActive(id, active) {
    return handleResponse(await apiFetch(`${BASE_URL}/${id}/active`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ active }),
    }));
}

export async function deleteDevice(id) {
    return handleResponse(await apiFetch(`${BASE_URL}/${id}`, {
        method: "DELETE",
    }));
}
