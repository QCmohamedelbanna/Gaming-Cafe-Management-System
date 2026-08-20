import { apiFetch, readApiResponse } from "./http";

export async function searchCustomers(query) {
    const suffix = query ? `?q=${encodeURIComponent(query)}` : "";
    return readApiResponse(await apiFetch(`/customers${suffix}`, { cache: "no-store" }));
}
