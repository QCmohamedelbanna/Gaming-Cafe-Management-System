import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { LanguageProvider } from "../../i18n";
import ReservationsPage from "./ReservationsPage";
import * as reservationApi from "../../api/reservationApi";
import * as deviceApi from "../../api/deviceApi";
import * as pricingApi from "../../api/pricingApi";

vi.mock("../../api/reservationApi");
vi.mock("../../api/deviceApi");
vi.mock("../../api/pricingApi");

const device = { id: 1, name: "PS4-1", type: "PS4", status: "AVAILABLE", active: true };

const reservation = {
    id: 5,
    customer: { id: 1, name: "Ahmed", phone: "01000000001" },
    device,
    sessionType: "SINGLE",
    startTime: "2027-01-01T18:00:00",
    durationMinutes: 60,
    status: "UPCOMING",
};

function renderPage() {
    return render(
        <LanguageProvider>
            <ReservationsPage />
        </LanguageProvider>
    );
}

afterEach(() => {
    vi.resetAllMocks();
});

describe("ReservationsPage", () => {
    it("loads and displays upcoming reservations", async () => {
        reservationApi.getUpcomingReservations.mockResolvedValue([reservation]);
        deviceApi.getDevices.mockResolvedValue([device]);
        pricingApi.getPricing.mockResolvedValue([]);
        renderPage();

        expect(await screen.findByText("Ahmed")).toBeInTheDocument();
        expect(screen.getByText("01000000001")).toBeInTheDocument();
        expect(screen.getByText("PS4-1")).toBeInTheDocument();
    });

    it("shows the empty state when there are no upcoming reservations", async () => {
        reservationApi.getUpcomingReservations.mockResolvedValue([]);
        deviceApi.getDevices.mockResolvedValue([device]);
        pricingApi.getPricing.mockResolvedValue([]);
        renderPage();

        expect(await screen.findByText("No upcoming reservations")).toBeInTheDocument();
    });

    it("cancels a reservation and removes it from the list", async () => {
        const user = userEvent.setup();
        reservationApi.getUpcomingReservations.mockResolvedValue([reservation]);
        deviceApi.getDevices.mockResolvedValue([device]);
        pricingApi.getPricing.mockResolvedValue([]);
        reservationApi.cancelReservation.mockResolvedValue({ ...reservation, status: "CANCELLED" });
        renderPage();
        await screen.findByText("Ahmed");

        await user.click(screen.getByRole("button", { name: "Cancel" }));
        const dialog = screen.getByRole("alertdialog");
        await user.click(within(dialog).getByRole("button", { name: "Cancel" }));

        await waitFor(() => expect(screen.queryByText("Ahmed")).not.toBeInTheDocument());
        expect(reservationApi.cancelReservation).toHaveBeenCalledWith(5, null);
        expect(screen.getByText("Ahmed's reservation was cancelled.")).toBeInTheDocument();
    });

    it("checks in a reservation, starting a session for its device", async () => {
        const user = userEvent.setup();
        reservationApi.getUpcomingReservations.mockResolvedValue([reservation]);
        deviceApi.getDevices.mockResolvedValue([device]);
        pricingApi.getPricing.mockResolvedValue([
            { deviceType: "PS4", sessionType: "SINGLE", billingUnit: "HOUR", price: 40, active: true },
        ]);
        reservationApi.checkInReservation.mockResolvedValue({ ...reservation, status: "CHECKED_IN" });
        renderPage();
        await screen.findByText("Ahmed");

        await user.click(screen.getByRole("button", { name: "Check in" }));
        await user.click(screen.getByRole("button", { name: /^Single/ }));
        await user.click(screen.getByRole("button", { name: "1 hour" }));
        await user.click(screen.getByRole("button", { name: "Start session" }));

        await waitFor(() =>
            expect(reservationApi.checkInReservation).toHaveBeenCalledWith(5, {
                deviceId: 1,
                sessionType: "SINGLE",
                plannedMinutes: 60,
                matchCount: null,
            })
        );
        await waitFor(() => expect(screen.queryByText("Ahmed")).not.toBeInTheDocument());
        expect(screen.getByText("Ahmed checked in.")).toBeInTheDocument();
    });

    it("shows the load error message when a dependent API call fails", async () => {
        reservationApi.getUpcomingReservations.mockRejectedValue(new Error("Network error"));
        deviceApi.getDevices.mockResolvedValue([]);
        pricingApi.getPricing.mockResolvedValue([]);
        renderPage();

        expect(await screen.findByText("Network error")).toBeInTheDocument();
    });
});
