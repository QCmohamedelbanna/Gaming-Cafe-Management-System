const BASE_URL = "http://localhost:8080/api/bills";

async function handleResponse(response) {
    const text = await response.text();

    if (!response.ok) {
        let message = text;
        try {
            message = JSON.parse(text).message || text;
        } catch {
            // Keep plain-text API errors.
        }
        throw new Error(message || "Billing request failed");
    }

    return text ? JSON.parse(text) : null;
}

export async function getBill(id) {
    return handleResponse(await fetch(`${BASE_URL}/${id}`));
}

export async function getPendingBills() {
    return handleResponse(await fetch(`${BASE_URL}/pending`));
}

export async function payBill(id, data) {
    return handleResponse(await fetch(`${BASE_URL}/${id}/pay`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
    }));
}

export async function cancelBill(id) {
    return handleResponse(await fetch(`${BASE_URL}/${id}/cancel`, {
        method: "POST",
    }));
}

export async function refundBill(id, reason) {
    return handleResponse(await fetch(`${BASE_URL}/${id}/refund`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ reason }),
    }));
}
