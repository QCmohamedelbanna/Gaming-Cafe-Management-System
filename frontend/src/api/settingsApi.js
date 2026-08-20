import { apiFetch, readApiResponse } from "./http";

export async function getSettings() {
    return readApiResponse(await apiFetch("/settings", { cache: "no-store" }));
}

export async function updateSettings(data) {
    return readApiResponse(await apiFetch("/settings", {
        method: "PUT",
        body: JSON.stringify(data),
    }));
}
