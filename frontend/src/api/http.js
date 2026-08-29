// Production is same-origin. Vite's development proxy handles the same
// relative path while the frontend is served from port 5173.
const configuredApiBaseUrl = import.meta.env.VITE_API_URL?.trim();
const API_BASE_URL = configuredApiBaseUrl || "/api";

const unsafeMethods = new Set(["POST", "PUT", "PATCH", "DELETE"]);

export function resolveApiUrl(path) {
    if (/^https?:\/\//i.test(path)) return path;
    return `${API_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`;
}

function readCookie(name) {
    const prefix = `${name}=`;
    return document.cookie
        .split(";")
        .map((part) => part.trim())
        .find((part) => part.startsWith(prefix))
        ?.slice(prefix.length) || "";
}

export async function ensureCsrf() {
    const response = await fetch(resolveApiUrl("/auth/csrf"), {
        method: "GET",
        credentials: "include",
        cache: "no-store",
    });
    return readApiResponse(response);
}

export async function apiFetch(path, options = {}) {
    const method = String(options.method || "GET").toUpperCase();
    const headers = new Headers(options.headers || {});

    if (options.body && !(options.body instanceof FormData) && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    if (unsafeMethods.has(method) && !headers.has("X-XSRF-TOKEN")) {
        let csrfToken = readCookie("XSRF-TOKEN");
        if (!csrfToken) {
            await ensureCsrf();
            csrfToken = readCookie("XSRF-TOKEN");
        }
        if (csrfToken) headers.set("X-XSRF-TOKEN", decodeURIComponent(csrfToken));
    }

    return fetch(resolveApiUrl(path), {
        ...options,
        method,
        headers,
        credentials: "include",
    });
}

export async function readApiResponse(response) {
    const text = await response.text();
    let payload = null;

    if (text) {
        try {
            payload = JSON.parse(text);
        } catch {
            payload = text;
        }
    }

    if (!response.ok) {
        const message = typeof payload === "object" && payload?.message
            ? payload.message
            : typeof payload === "string" && payload
                ? payload
                : response.status === 401
                    ? "Authentication required"
                    : response.status === 403
                        ? "You do not have permission to perform this action"
                        : "Request failed";
        const error = new Error(message);
        error.status = response.status;
        throw error;
    }

    return payload;
}

export { API_BASE_URL };
