import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { LanguageProvider } from "../../i18n";
import ProductFormModal from "./ProductFormModal";

function renderForm(props) {
    return render(
        <LanguageProvider>
            <ProductFormModal {...props} />
        </LanguageProvider>
    );
}

describe("ProductFormModal", () => {
    it("disables save until a name and a positive selling price are entered", async () => {
        const user = userEvent.setup();
        renderForm({ onSave: vi.fn(), onClose: vi.fn() });

        const save = screen.getByRole("button", { name: "Add product" });
        expect(save).toBeDisabled();

        await user.type(screen.getByLabelText("Product name"), "Iced Tea");
        expect(save).toBeDisabled();

        await user.type(screen.getByLabelText("Selling price"), "15");
        expect(save).toBeEnabled();
    });

    it("rejects a zero selling price", async () => {
        const user = userEvent.setup();
        renderForm({ onSave: vi.fn(), onClose: vi.fn() });

        await user.type(screen.getByLabelText("Product name"), "Iced Tea");
        await user.type(screen.getByLabelText("Selling price"), "0");

        expect(screen.getByRole("button", { name: "Add product" })).toBeDisabled();
    });

    it("submits the expected payload for a new product", async () => {
        const user = userEvent.setup();
        const onSave = vi.fn();
        renderForm({ onSave, onClose: vi.fn() });

        await user.type(screen.getByLabelText("Product name"), "  Iced Tea  ");
        await user.type(screen.getByLabelText("Selling price"), "15");
        await user.click(screen.getByRole("button", { name: "Add product" }));

        expect(onSave).toHaveBeenCalledWith({
            name: "Iced Tea",
            price: 15,
            sellingPrice: 15,
            sku: null,
            category: "Uncategorized",
            costPrice: 0,
            trackStock: true,
            minimumStock: 0,
            unit: "unit",
        });
    });

    it("pre-fills fields from an existing product when editing", () => {
        renderForm({
            product: {
                id: 1,
                name: "Cola",
                sku: "COLA-1",
                category: "Drinks",
                sellingPrice: 20,
                costPrice: 8,
                trackStock: true,
                minimumStock: 5,
                unit: "can",
            },
            onSave: vi.fn(),
            onClose: vi.fn(),
        });

        expect(screen.getByLabelText("Product name")).toHaveValue("Cola");
        expect(screen.getByLabelText("Selling price")).toHaveValue(20);
        expect(screen.getByRole("button", { name: "Save" })).toBeEnabled();
    });

    it("disables the form while saving", () => {
        renderForm({
            product: { name: "Cola", sellingPrice: 20 },
            saving: true,
            onSave: vi.fn(),
            onClose: vi.fn(),
        });

        expect(screen.getByRole("button", { name: "Working..." })).toBeDisabled();
        expect(screen.getByRole("button", { name: "Cancel" })).toBeDisabled();
    });

    it("calls onClose when cancel is clicked", async () => {
        const user = userEvent.setup();
        const onClose = vi.fn();
        renderForm({ onSave: vi.fn(), onClose });

        await user.click(screen.getByRole("button", { name: "Cancel" }));
        expect(onClose).toHaveBeenCalled();
    });
});
