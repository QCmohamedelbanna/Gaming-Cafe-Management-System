import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { LanguageProvider } from "../../i18n";
import SettingsPage from "./SettingsPage";
import * as settingsApi from "../../api/settingsApi";

vi.mock("../../api/settingsApi");

const defaults = {
    preventNegativeStock: true,
    discountAllowedRoles: ["ADMIN", "MANAGER"],
    dashboardEndingSoonMinutes: 30,
    reservationsNoShowGraceMinutes: 20,
};

function renderPage() {
    return render(
        <LanguageProvider>
            <SettingsPage />
        </LanguageProvider>
    );
}

afterEach(() => {
    vi.resetAllMocks();
});

describe("SettingsPage", () => {
    it("loads and displays the current settings", async () => {
        settingsApi.getSettings.mockResolvedValue(defaults);
        renderPage();

        expect(await screen.findByRole("checkbox", { name: /Prevent negative stock/ })).toBeChecked();
        expect(screen.getByRole("checkbox", { name: "Admin" })).toBeChecked();
        expect(screen.getByRole("checkbox", { name: "Manager" })).toBeChecked();
        expect(screen.getByRole("checkbox", { name: "Cashier" })).not.toBeChecked();
        expect(screen.getByLabelText(/ending soon" threshold/)).toHaveValue(30);
    });

    it("saves the edited settings", async () => {
        const user = userEvent.setup();
        settingsApi.getSettings.mockResolvedValue(defaults);
        settingsApi.updateSettings.mockResolvedValue({
            ...defaults,
            preventNegativeStock: false,
            discountAllowedRoles: ["ADMIN", "MANAGER", "CASHIER"],
        });
        renderPage();
        await screen.findByRole("checkbox", { name: /Prevent negative stock/ });

        await user.click(screen.getByRole("checkbox", { name: /Prevent negative stock/ }));
        await user.click(screen.getByRole("checkbox", { name: "Cashier" }));
        await user.click(screen.getByRole("button", { name: "Save" }));

        await waitFor(() =>
            expect(settingsApi.updateSettings).toHaveBeenCalledWith({
                preventNegativeStock: false,
                discountAllowedRoles: ["ADMIN", "MANAGER", "CASHIER"],
                dashboardEndingSoonMinutes: 30,
                reservationsNoShowGraceMinutes: 20,
            })
        );
        expect(await screen.findByText("Settings saved.")).toBeInTheDocument();
    });

    it("disables save and shows a warning when every discount role is unchecked", async () => {
        const user = userEvent.setup();
        settingsApi.getSettings.mockResolvedValue({ ...defaults, discountAllowedRoles: ["ADMIN"] });
        renderPage();
        await screen.findByRole("checkbox", { name: "Admin" });

        await user.click(screen.getByRole("checkbox", { name: "Admin" }));

        expect(screen.getByText("At least one role must be allowed to apply discounts.")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Save" })).toBeDisabled();
    });

    it("shows a save error from the server", async () => {
        const user = userEvent.setup();
        settingsApi.getSettings.mockResolvedValue(defaults);
        settingsApi.updateSettings.mockRejectedValue(new Error("Could not save settings."));
        renderPage();
        await screen.findByRole("checkbox", { name: /Prevent negative stock/ });

        await user.click(screen.getByRole("button", { name: "Save" }));

        expect(await screen.findByText("Could not save settings.")).toBeInTheDocument();
    });

    it("shows the load error message when settings fail to load", async () => {
        settingsApi.getSettings.mockRejectedValue(new Error("Network error"));
        renderPage();

        expect(await screen.findByText("Network error")).toBeInTheDocument();
    });
});
