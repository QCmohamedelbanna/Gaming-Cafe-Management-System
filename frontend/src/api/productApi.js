const BASE_URL =
    "http://localhost:8080/api/products";

async function handleResponse(response) {

    if (!response.ok) {
        const message = await response.text();

        throw new Error(
            message || "Product request failed"
        );
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

export async function getProducts() {

    const response =
        await fetch(BASE_URL);

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

export async function deleteProduct(id) {

    const response =
        await fetch(
            `${BASE_URL}/${id}`,
            {
                method: "DELETE",
            }
        );

    if (!response.ok) {
        const message = await response.text();

        throw new Error(
            message || "Could not delete product"
        );
    }
}