const BASE_URL = "http://localhost:8080/api/orders";
const CURRENT_USER_ROLE = "ADMIN";
const CURRENT_CASHIER = "Admin";

async function handleResponse(response) {
    const text = await response.text();

    if (!response.ok) {
        let message = text;
        try {
            const payload = JSON.parse(text);
            message = payload.message || payload.error || text;
        } catch {
            // Keep plain-text API errors as-is.
        }
        throw new Error(message || "Request failed");
    }

    return text ? JSON.parse(text) : null;
}

export async function createOrder(gameSessionId = null) {
    const response = await fetch(BASE_URL, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            gameSessionId,
        }),
    });

    return handleResponse(response);
}

export async function addOrderItem(
    orderId,
    productId,
    quantity = 1
) {
    const response = await fetch(
        `${BASE_URL}/${orderId}/items`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                productId,
                quantity,
            }),
        }
    );

    return handleResponse(response);
}

export async function removeOrderItem(
    orderId,
    itemId
) {
    const response = await fetch(
        `${BASE_URL}/${orderId}/items/${itemId}`,
        {
            method: "DELETE",
        }
    );

    return handleResponse(response);
}

export async function updateOrderItemQuantity(
    orderId,
    itemId,
    quantity
) {
    const response = await fetch(
        `${BASE_URL}/${orderId}/items/${itemId}/quantity`,
        {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json",
                "X-Cashier": CURRENT_CASHIER,
            },
            body: JSON.stringify({ quantity }),
        }
    );

    return handleResponse(response);
}

export async function applyOrderDiscount(orderId, data) {
    const response = await fetch(`${BASE_URL}/${orderId}/discount`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
            "X-User-Role": CURRENT_USER_ROLE,
        },
        body: JSON.stringify(data),
    });

    return handleResponse(response);
}

export async function clearOrderDiscount(orderId) {
    const response = await fetch(`${BASE_URL}/${orderId}/discount`, {
        method: "DELETE",
    });

    return handleResponse(response);
}

export async function holdOrder(orderId) {
    return handleResponse(await fetch(`${BASE_URL}/${orderId}/hold`, {
        method: "POST",
    }));
}

export async function resumeOrder(orderId) {
    return handleResponse(await fetch(`${BASE_URL}/${orderId}/resume`, {
        method: "POST",
    }));
}

export async function cancelOrder(orderId) {
    return handleResponse(await fetch(`${BASE_URL}/${orderId}/cancel`, {
        method: "POST",
    }));
}

export async function getHeldOrders() {
    return handleResponse(await fetch(`${BASE_URL}/held`));
}

export async function completeOrder(orderId, payment) {
    const response = await fetch(
        `${BASE_URL}/${orderId}/complete`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-Cashier": CURRENT_CASHIER,
            },
            body: JSON.stringify(payment),
        }
    );

    return handleResponse(response);

}

export async function getOpenOrderForSession(sessionId) {
    const response = await fetch(
        `${BASE_URL}/session/${sessionId}/open`,
        { cache: "no-store" }
    );

    if (!response.ok) {
        const message = await response.text();

        throw new Error(
            message || "Failed to load session order"
        );
    }

    const text = await response.text();

    return text ? JSON.parse(text) : null;
}
