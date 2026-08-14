import { apiFetch, readApiResponse } from "./http";

export async function getUsers() {
    return readApiResponse(await apiFetch("/users", { cache: "no-store" }));
}

export async function createUser(data) {
    return readApiResponse(await apiFetch("/users", {
        method: "POST",
        body: JSON.stringify(data),
    }));
}

export async function updateUser(id, data) {
    return readApiResponse(await apiFetch(`/users/${id}`, {
        method: "PUT",
        body: JSON.stringify(data),
    }));
}
