import { apiFetch } from "./http";

const BASE_URL = "/devices";

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

export async function configureDeviceControl(id, data) {
    return handleResponse(await apiFetch(`${BASE_URL}/${id}/control`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
    }));
}

export async function getDevicePower(id) {
    return handleResponse(await apiFetch(`${BASE_URL}/${id}/power`, { cache: "no-store" }));
}

export async function powerOnDevice(id) {
    return handleResponse(await apiFetch(`${BASE_URL}/${id}/power/on`, { method: "POST" }));
}

export async function powerOffDevice(id) {
    return handleResponse(await apiFetch(`${BASE_URL}/${id}/power/off`, { method: "POST" }));
}

export async function getDevicePowerDiagnostics(id) {
    return handleResponse(await apiFetch(`${BASE_URL}/${id}/power/diagnostics`, { cache: "no-store" }));
}
