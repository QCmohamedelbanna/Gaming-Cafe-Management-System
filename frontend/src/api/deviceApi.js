const BASE_URL = "http://localhost:8080/api/devices";

export async function getDevices() {
    const response = await fetch(BASE_URL);

    if (!response.ok) {
        throw new Error("Failed to load devices");
    }

    return response.json();
}