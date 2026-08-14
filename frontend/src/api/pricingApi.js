import { apiFetch } from "./http";

const BASE_URL = "http://localhost:8080/api/pricing";

export async function getPricing() {
  const response = await apiFetch(BASE_URL, { cache: "no-store" });

  if (!response.ok) {
    throw new Error("Failed to load pricing");
  }

  return response.json();
}

export async function updatePricing(id, data) {
  const response = await apiFetch(`${BASE_URL}/${id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || "Failed to update pricing");
  }

  return response.json();
}
