const BASE_URL = "http://localhost:8080/api/orders";

async function handleResponse(response) {
    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || "Request failed");
    }

    return response.json();
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

export async function completeOrder(orderId) {
    const response = await fetch(
        `${BASE_URL}/${orderId}/complete`,
        {
            method: "POST",
        }
    );

    return handleResponse(response);
}