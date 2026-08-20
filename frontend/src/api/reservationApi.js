import { apiFetch, readApiResponse } from "./http";

export async function getUpcomingReservations() {
    return readApiResponse(await apiFetch("/reservations", { cache: "no-store" }));
}

export async function getAllReservations() {
    return readApiResponse(await apiFetch("/reservations?all=true", { cache: "no-store" }));
}

export async function createReservation(data) {
    return readApiResponse(await apiFetch("/reservations", {
        method: "POST",
        body: JSON.stringify(data),
    }));
}

export async function checkInReservation(id, data) {
    return readApiResponse(await apiFetch(`/reservations/${id}/check-in`, {
        method: "POST",
        body: JSON.stringify(data),
    }));
}

export async function cancelReservation(id, reason) {
    return readApiResponse(await apiFetch(`/reservations/${id}/cancel`, {
        method: "POST",
        body: JSON.stringify({ reason }),
    }));
}
