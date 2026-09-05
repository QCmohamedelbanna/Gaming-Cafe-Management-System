import "@testing-library/jest-dom/vitest";

// jsdom can omit storage when the test environment has an opaque origin.
// Keep the existing language preference code deterministic in that case.
if (typeof window.localStorage === "undefined") {
    const values = new Map();
    Object.defineProperty(window, "localStorage", {
        configurable: true,
        value: {
            getItem: (key) => values.get(key) ?? null,
            setItem: (key, value) => values.set(key, String(value)),
            removeItem: (key) => values.delete(key),
            clear: () => values.clear(),
        },
    });
}
