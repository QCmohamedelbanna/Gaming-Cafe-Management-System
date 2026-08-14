import { apiFetch, readApiResponse } from "./http";

export async function getCurrentShift() {
    return readApiResponse(await apiFetch("/shifts/current", { cache: "no-store" }));
}

export async function getMyShifts() {
    return readApiResponse(await apiFetch("/shifts/mine", { cache: "no-store" }));
}

export async function getAllShifts() {
    return readApiResponse(await apiFetch("/shifts", { cache: "no-store" }));
}

export async function openShift(openingCash) {
    return readApiResponse(await apiFetch("/shifts", {
        method: "POST",
        body: JSON.stringify({ openingCash: Number(openingCash) }),
    }));
}

export async function closeShift(actualCash, note = "") {
    return readApiResponse(await apiFetch("/shifts/close", {
        method: "POST",
        body: JSON.stringify({ actualCash: Number(actualCash), note }),
    }));
}

export async function getShiftReport(id) {
    return readApiResponse(await apiFetch(`/shifts/${id}/report`, { cache: "no-store" }));
}
