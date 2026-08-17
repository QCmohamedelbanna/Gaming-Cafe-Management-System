import { apiFetch, readApiResponse } from "./http";

export async function getRules() {
    return readApiResponse(await apiFetch("/rules", { cache: "no-store" }));
}

export async function createRule(data) {
    return readApiResponse(await apiFetch("/rules", {
        method: "POST",
        body: JSON.stringify(data),
    }));
}

export async function updateRule(id, data) {
    return readApiResponse(await apiFetch("/rules/" + id, {
        method: "PUT",
        body: JSON.stringify(data),
    }));
}

export async function deleteRule(id) {
    return readApiResponse(await apiFetch("/rules/" + id, {
        method: "DELETE",
    }));
}
