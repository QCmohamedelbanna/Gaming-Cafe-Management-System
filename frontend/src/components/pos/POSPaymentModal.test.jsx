import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { LanguageProvider } from "../../i18n";
import POSPaymentModal from "./POSPaymentModal";

function renderModal(props) {
    return render(
        <LanguageProvider>
            <POSPaymentModal order={{ id: 5, totalAmount: 40, subtotalAmount: 40, discountAmount: 0 }} {...props} />
        </LanguageProvider>
    );
}

describe("POSPaymentModal", () => {
    it("keeps cash payment disabled until the amount received covers the total", async () => {
        const user = userEvent.setup();
        renderModal();

        const payButton = screen.getByRole("button", { name: /Pay/ });
        expect(payButton).toBeDisabled();

        await user.type(screen.getByLabelText("Amount received"), "40");
        expect(payButton).toBeEnabled();
    });

    it("shows the change due once the cash received covers the total", async () => {
        const user = userEvent.setup();
        renderModal();

        await user.type(screen.getByLabelText("Amount received"), "50");
        expect(screen.getByText(/Change:/)).toBeInTheDocument();
    });

    it("allows card and mobile-wallet payment without requiring an amount", async () => {
        const user = userEvent.setup();
        renderModal();

        await user.click(screen.getByRole("button", { name: "Card" }));
        expect(screen.getByRole("button", { name: /Pay/ })).toBeEnabled();
    });

    it("submits cash payment with the tendered amount", async () => {
        const user = userEvent.setup();
        const onConfirm = vi.fn();
        renderModal({ onConfirm });

        await user.type(screen.getByLabelText("Amount received"), "50");
        await user.click(screen.getByRole("button", { name: /Pay/ }));

        expect(onConfirm).toHaveBeenCalledWith({ paymentMethod: "CASH", amountTendered: 50 });
    });

    it("submits card payment with a null tendered amount", async () => {
        const user = userEvent.setup();
        const onConfirm = vi.fn();
        renderModal({ onConfirm });

        await user.click(screen.getByRole("button", { name: "Card" }));
        await user.click(screen.getByRole("button", { name: /Pay/ }));

        expect(onConfirm).toHaveBeenCalledWith({ paymentMethod: "CARD", amountTendered: null });
    });

    it("shows a server-side payment error", () => {
        renderModal({ error: "Amount tendered is less than the bill total" });

        expect(screen.getByRole("alert")).toHaveTextContent(
            "Amount tendered is less than the bill total"
        );
    });

    it("disables every control while a payment is in flight", () => {
        renderModal({ loading: true });

        expect(screen.getByRole("button", { name: /Processing/ })).toBeDisabled();
        expect(screen.getByRole("button", { name: "Back" })).toBeDisabled();
    });
});
