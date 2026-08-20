import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { LanguageProvider } from "../../i18n";
import ProductsPage from "./ProductsPage";
import * as productApi from "../../api/productApi";

vi.mock("../../api/productApi");

function renderPage() {
    return render(
        <LanguageProvider>
            <ProductsPage />
        </LanguageProvider>
    );
}

const cola = {
    id: 1,
    name: "Cola",
    sellingPrice: 20,
    active: true,
};

afterEach(() => {
    vi.resetAllMocks();
});

describe("ProductsPage", () => {
    it("loads and displays products from the API", async () => {
        productApi.getAdminProducts.mockResolvedValue([cola]);
        renderPage();

        expect(await screen.findByText("Cola")).toBeInTheDocument();
    });

    it("shows a duplicate-name save error on the page, not inside the form modal", async () => {
        const user = userEvent.setup();
        productApi.getAdminProducts.mockResolvedValue([cola]);
        productApi.createProduct.mockRejectedValue(
            new Error("A product with this name already exists")
        );
        renderPage();
        await screen.findByText("Cola");

        await user.click(screen.getByRole("button", { name: "+ Add product" }));
        const dialog = screen.getByRole("dialog");
        await user.type(within(dialog).getByLabelText("Product name"), "Cola");
        await user.type(within(dialog).getByLabelText("Selling price"), "20");
        await user.click(within(dialog).getByRole("button", { name: "Add product" }));

        expect(await screen.findByText("A product with this name already exists")).toBeInTheDocument();
        // The error is rendered on the page, outside the (still-open) dialog.
        expect(within(dialog).queryByText("A product with this name already exists")).not.toBeInTheDocument();
        expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    it("creates a product successfully and shows a confirmation message", async () => {
        const user = userEvent.setup();
        productApi.getAdminProducts.mockResolvedValue([]);
        productApi.createProduct.mockResolvedValue({
            id: 2, name: "Iced Tea", sellingPrice: 15, active: true,
        });
        renderPage();
        await screen.findByText("No products yet");

        await user.click(screen.getByRole("button", { name: "+ Add product" }));
        await user.type(screen.getByLabelText("Product name"), "Iced Tea");
        await user.type(screen.getByLabelText("Selling price"), "15");
        await user.click(screen.getByRole("button", { name: "Add product" }));

        expect(await screen.findByText("Iced Tea saved successfully.")).toBeInTheDocument();
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });

    it("shows a delete-blocked error inside the confirmation dialog and keeps it open", async () => {
        const user = userEvent.setup();
        productApi.getAdminProducts.mockResolvedValue([cola]);
        productApi.deleteProduct.mockRejectedValue(
            new Error("This product is in an open order. Complete checkout before deleting it.")
        );
        renderPage();
        await screen.findByText("Cola");

        await user.click(screen.getByRole("button", { name: "Delete" }));
        const dialog = screen.getByRole("alertdialog");
        await user.click(within(dialog).getByRole("button", { name: "Delete" }));

        await waitFor(() =>
            expect(within(dialog).getByRole("alert")).toHaveTextContent(
                "This product is in an open order. Complete checkout before deleting it."
            )
        );
        expect(screen.getByRole("alertdialog")).toBeInTheDocument();
    });

    it("removes the product from the list after a successful delete", async () => {
        const user = userEvent.setup();
        productApi.getAdminProducts.mockResolvedValue([cola]);
        productApi.deleteProduct.mockResolvedValue(undefined);
        renderPage();
        await screen.findByText("Cola");

        await user.click(screen.getByRole("button", { name: "Delete" }));
        await user.click(within(screen.getByRole("alertdialog")).getByRole("button", { name: "Delete" }));

        await waitFor(() => expect(screen.queryByText("Cola")).not.toBeInTheDocument());
        expect(screen.getByText("Cola deleted.")).toBeInTheDocument();
    });

    it("shows the generic load error message when the API call fails", async () => {
        productApi.getAdminProducts.mockRejectedValue(new Error("Network error"));
        renderPage();

        expect(await screen.findByText("Network error")).toBeInTheDocument();
    });
});
