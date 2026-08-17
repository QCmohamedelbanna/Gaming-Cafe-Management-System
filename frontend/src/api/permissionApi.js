import { apiFetch, readApiResponse } from "./http";

export async function getPermissions() {
    return readApiResponse(await apiFetch("/permissions", { cache: "no-store" }));
}

export async function updateRolePermissions(role, permissions) {
    return readApiResponse(await apiFetch(`/permissions/roles/${role}`, {
        method: "PUT",
        body: JSON.stringify({ permissions }),
    }));
}
