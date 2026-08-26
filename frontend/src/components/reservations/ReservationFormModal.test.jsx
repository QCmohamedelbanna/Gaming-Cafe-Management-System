import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { LanguageProvider } from "../../i18n";
import ReservationFormModal from "./ReservationFormModal";
import * as customerApi from "../../api/customerApi";

vi.mock("../../api/customerApi");

const devices = [
    { id: 1, name: "PS4-1", type: "PS4" },
    { id: 2, name: "PS5-1", type: "PS5" },
];

function renderForm(props) {
    return render(
        <LanguageProvider>
            <ReservationFormModal devices={devices} onSave={vi.fn()} onClose={vi.fn()} {...props} />
        </LanguageProvider>
    );
}

afterEach(() => {
    vi.resetAllMocks();
});

describe("ReservationFormModal", () => {
    it("disables save until phone, name, and a start time are entered", async () => {
        const user = userEvent.setup();
        renderForm();

        const save = screen.getByRole("button", { name: "New reservation" });
        expect(save).toBeDisabled();

        await user.type(screen.getByLabelText("Phone number"), "01000000001");
        await user.type(screen.getByLabelText("Customer name"), "Ahmed");
        expect(save).toBeDisabled();

        const startTime = screen.getByLabelText("Start time");
        await user.type(startTime, "2027-01-01T18:00");
        expect(save).toBeEnabled();
    });

    it("submits the expected payload", async () => {
        const user = userEvent.setup();
        const onSave = vi.fn();
        renderForm({ onSave });

        await user.type(screen.getByLabelText("Phone number"), "01000000002");
        await user.type(screen.getByLabelText("Customer name"), "Sara");
        await user.type(screen.getByLabelText("Start time"), "2027-01-01T18:00");
        await user.click(screen.getByRole("button", { name: "New reservation" }));

        expect(onSave).toHaveBeenCalledWith({
            customerName: "Sara",
            customerPhone: "01000000002",
            deviceId: 1,
            sessionType: "SINGLE",
            startTime: "2027-01-01T18:00",
            durationMinutes: 60,
            notes: null,
        });
    });

    it("submits an open-ended duration when Open time is selected", async () => {
        const user = userEvent.setup();
        const onSave = vi.fn();
        renderForm({ onSave });

        await user.type(screen.getByLabelText("Phone number"), "01000000005");
        await user.type(screen.getByLabelText("Customer name"), "Omar");
        await user.type(screen.getByLabelText("Start time"), "2027-01-01T18:00");
        await user.selectOptions(screen.getByLabelText("Duration"), "OPEN_TIME");
        await user.click(screen.getByRole("button", { name: "New reservation" }));

        expect(onSave).toHaveBeenCalledWith(expect.objectContaining({
            durationMinutes: null,
        }));
    });

    it("looks up an existing customer by phone and fills in their name", async () => {
        const user = userEvent.setup();
        customerApi.searchCustomers.mockResolvedValue([
            { id: 9, name: "Existing Customer", phone: "01000000003" },
        ]);
        renderForm();

        await user.type(screen.getByLabelText("Phone number"), "01000000003");
        await user.tab();

        await waitFor(() =>
            expect(screen.getByLabelText("Customer name")).toHaveValue("Existing Customer")
        );
    });

    it("does not overwrite a name the user already typed", async () => {
        const user = userEvent.setup();
        customerApi.searchCustomers.mockResolvedValue([
            { id: 9, name: "Existing Customer", phone: "01000000004" },
        ]);
        renderForm();

        await user.type(screen.getByLabelText("Customer name"), "Typed Name");
        await user.type(screen.getByLabelText("Phone number"), "01000000004");
        await user.tab();

        expect(customerApi.searchCustomers).not.toHaveBeenCalled();
        expect(screen.getByLabelText("Customer name")).toHaveValue("Typed Name");
    });

    it("calls onClose when cancelled", async () => {
        const user = userEvent.setup();
        const onClose = vi.fn();
        renderForm({ onClose });

        await user.click(screen.getByRole("button", { name: "Cancel" }));
        expect(onClose).toHaveBeenCalled();
    });

    it("shows a server-side create error", () => {
        renderForm({ error: "This device already has a reservation that overlaps this time" });

        expect(screen.getByRole("alert")).toHaveTextContent(
            "This device already has a reservation that overlaps this time"
        );
    });
});
