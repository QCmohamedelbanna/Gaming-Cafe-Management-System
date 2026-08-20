import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { LanguageProvider } from "../../i18n";
import OrderCart from "./OrderCart";

function renderCart(props) {
    return render(
        <LanguageProvider>
            <OrderCart {...props} />
        </LanguageProvider>
    );
}

function buildOrder(overrides = {}) {
    return {
        id: 42,
        items: [
            {
                id: 1,
                product: { id: 10, name: "Cola", trackStock: false, currentStock: 0 },
                quantity: 2,
                unitPriceSnapshot: 5,
                lineTotal: 10,
            },
        ],
        subtotalAmount: 10,
        discountAmount: 0,
        totalAmount: 10,
        ...overrides,
    };
}

describe("OrderCart", () => {
    it("shows the standby state and no items when there is no order", () => {
        renderCart({ order: null });

        expect(screen.getByText("CURRENT ORDER")).toBeInTheDocument();
        expect(screen.getByText("No active order")).toBeInTheDocument();
    });

    it("increases and decreases item quantity through the +/- controls", async () => {
        const user = userEvent.setup();
        const onUpdateQuantity = vi.fn();
        renderCart({ order: buildOrder(), onUpdateQuantity });

        await user.click(screen.getByRole("button", { name: "Increase Cola" }));
        expect(onUpdateQuantity).toHaveBeenCalledWith(1, 3);

        await user.click(screen.getByRole("button", { name: "Decrease Cola" }));
        expect(onUpdateQuantity).toHaveBeenCalledWith(1, 1);
    });

    it("disables increasing quantity once the tracked stock limit is reached", () => {
        renderCart({
            order: buildOrder({
                items: [
                    {
                        id: 1,
                        product: { id: 10, name: "Cola", trackStock: true, currentStock: 2 },
                        quantity: 2,
                        unitPriceSnapshot: 5,
                        lineTotal: 10,
                    },
                ],
            }),
        });

        expect(screen.getByRole("button", { name: "Increase Cola" })).toBeDisabled();
        expect(screen.getByRole("button", { name: "Decrease Cola" })).toBeEnabled();
    });

    it("removes an item via the remove button", async () => {
        const user = userEvent.setup();
        const onRemove = vi.fn();
        renderCart({ order: buildOrder(), onRemove });

        await user.click(screen.getByRole("button", { name: "Remove Cola" }));
        expect(onRemove).toHaveBeenCalledWith(1);
    });

    it("disables checkout for an empty order and enables it once items exist", () => {
        const { rerender } = render(
            <LanguageProvider>
                <OrderCart order={buildOrder({ items: [] })} />
            </LanguageProvider>
        );
        expect(screen.getByRole("button", { name: "Complete order" })).toBeDisabled();

        rerender(
            <LanguageProvider>
                <OrderCart order={buildOrder()} />
            </LanguageProvider>
        );
        expect(screen.getByRole("button", { name: "Complete order" })).toBeEnabled();
    });

    it("locks checkout and hides hold/cancel for a session-attached order", () => {
        renderCart({
            order: buildOrder(),
            attachedSession: { id: 7, device: { name: "PS4-1" }, sessionType: "SINGLE" },
        });

        expect(screen.getByRole("button", { name: "Paid with session checkout" })).toBeDisabled();
        expect(screen.queryByRole("button", { name: "Hold order" })).not.toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Cancel" })).not.toBeInTheDocument();
    });

    it("calls onHold and onCancel for a standalone order", async () => {
        const user = userEvent.setup();
        const onHold = vi.fn();
        const onCancel = vi.fn();
        renderCart({ order: buildOrder(), onHold, onCancel });

        await user.click(screen.getByRole("button", { name: "Hold order" }));
        expect(onHold).toHaveBeenCalled();

        await user.click(screen.getByRole("button", { name: "Cancel" }));
        expect(onCancel).toHaveBeenCalled();
    });

    it("submits a discount with the entered type, value, and reason", async () => {
        const user = userEvent.setup();
        const onApplyDiscount = vi.fn();
        renderCart({ order: buildOrder(), onApplyDiscount });

        await user.click(screen.getByRole("button", { name: "Add discount" }));
        await user.type(screen.getByLabelText("Value"), "10");
        await user.type(screen.getByLabelText("Reason (optional)"), "Loyal customer");
        await user.click(screen.getByRole("button", { name: "Apply discount" }));

        expect(onApplyDiscount).toHaveBeenCalledWith({
            type: "PERCENTAGE",
            value: 10,
            reason: "Loyal customer",
        });
    });

    it("shows the discount error returned by the server", async () => {
        const user = userEvent.setup();
        renderCart({
            order: buildOrder(),
            discountError: "Discounts require manager or administrator permission",
        });

        await user.click(screen.getByRole("button", { name: "Add discount" }));
        expect(screen.getByRole("alert")).toHaveTextContent(
            "Discounts require manager or administrator permission"
        );
    });
});
