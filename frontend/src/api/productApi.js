const BASE_URL =
    "http://localhost:8080/api/products";

async function handleResponse(response) {

    const text = await response.text();

    if (!response.ok) {
        let message = text;

        try {
            message = JSON.parse(text).message || text;
        } catch {
            // Keep plain-text API errors as-is.
        }

        throw new Error(
            message || "Product request failed"
        );
    }

    if (!text) {
        return null;
    }

    return JSON.parse(text);
}

export async function getProducts() {

    const response =
        await fetch(BASE_URL);

    return handleResponse(response);
}

export async function getAdminProducts() {

    const response = await fetch(`${BASE_URL}/admin`);

    return handleResponse(response);
}

export async function createProduct(data) {

    const response =
        await fetch(BASE_URL, {
            method: "POST",

            headers: {
                "Content-Type": "application/json",
            },

            body: JSON.stringify(data),
        });

    return handleResponse(response);
}

export async function updateProduct(id, data) {

    const response =
        await fetch(
            `${BASE_URL}/${id}`,
            {
                method: "PUT",

                headers: {
                    "Content-Type": "application/json",
                },

                body: JSON.stringify(data),
            }
        );

    return handleResponse(response);
}

export async function setProductActive(id, active) {

    const response = await fetch(
        `${BASE_URL}/${id}/active`,
        {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ active }),
        }
    );

    return handleResponse(response);
}

export async function deleteProduct(id) {

    const response =
        await fetch(
            `${BASE_URL}/${id}`,
            {
                method: "DELETE",
            }
        );

    return handleResponse(response);
}
