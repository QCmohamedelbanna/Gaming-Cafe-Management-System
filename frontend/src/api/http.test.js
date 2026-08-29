import { describe, expect, it } from "vitest";
import { readApiResponse, resolveApiUrl } from "./http";

describe("readApiResponse", () => {
    it("falls back to a permission-denied message for a bodyless 403", async () => {
        const response = new Response("", { status: 403 });

        await expect(readApiResponse(response)).rejects.toMatchObject({
            message: "You do not have permission to perform this action",
            status: 403,
        });
    });

    it("falls back to an authentication-required message for a bodyless 401", async () => {
        const response = new Response("", { status: 401 });

        await expect(readApiResponse(response)).rejects.toMatchObject({
            message: "Authentication required",
            status: 401,
        });
    });

    it("prefers a server-provided JSON message over the generic fallback", async () => {
        const response = new Response(JSON.stringify({ message: "You lack the ADMIN role" }), {
            status: 403,
        });

        await expect(readApiResponse(response)).rejects.toMatchObject({
            message: "You lack the ADMIN role",
            status: 403,
        });
    });

    it("uses a plain-text error body when the response is not JSON", async () => {
        const response = new Response("Service unavailable", { status: 503 });

        await expect(readApiResponse(response)).rejects.toMatchObject({
            message: "Service unavailable",
            status: 503,
        });
    });

    it("returns the parsed JSON payload for a successful response", async () => {
        const response = new Response(JSON.stringify({ id: 1, name: "Cola" }), { status: 200 });

        await expect(readApiResponse(response)).resolves.toEqual({ id: 1, name: "Cola" });
    });
});

describe("resolveApiUrl", () => {
    it("passes an already-absolute URL through untouched", () => {
        expect(resolveApiUrl("https://example.com/api/products")).toBe(
            "https://example.com/api/products"
        );
    });

    it("prefixes a relative path with the configured API base URL", () => {
        expect(resolveApiUrl("/products")).toBe("/api/products");
        expect(resolveApiUrl("products")).toBe("/api/products");
    });
});
