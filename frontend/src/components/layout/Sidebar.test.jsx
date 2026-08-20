import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { LanguageProvider } from "../../i18n";
import { useAuth } from "../../auth/AuthContext";
import Sidebar from "./Sidebar";

vi.mock("../../auth/AuthContext");

function renderSidebar(permissions, props = {}) {
    useAuth.mockReturnValue({
        hasPermission: (...perms) => perms.some((p) => permissions.includes(p)),
    });
    return render(
        <LanguageProvider>
            <Sidebar activePage="pos" onNavigate={vi.fn()} collapsed={false} onToggle={vi.fn()} {...props} />
        </LanguageProvider>
    );
}

describe("Sidebar", () => {
    it("shows only the nav items the user has permission for", () => {
        renderSidebar(["POS_USE", "CHECKOUT_USE"]);

        expect(screen.getByRole("button", { name: /Point of Sale|POS/i })).toBeInTheDocument();
        expect(screen.getByText("Billing")).toBeInTheDocument();
        expect(screen.queryByText("Users")).not.toBeInTheDocument();
        expect(screen.queryByText("Products")).not.toBeInTheDocument();
        expect(screen.queryByText("Devices")).not.toBeInTheDocument();
    });

    it("shows every nav item for a user with every permission", () => {
        renderSidebar([
            "OPERATIONS_USE", "POS_USE", "DASHBOARD_VIEW", "DEVICES_MANAGE", "PRODUCTS_MANAGE",
            "CHECKOUT_USE", "INVENTORY_MANAGE", "PRICING_MANAGE", "REPORTS_VIEW", "SHIFT_MANAGE",
            "USERS_MANAGE", "PERMISSIONS_MANAGE", "SETTINGS_MANAGE",
        ]);

        expect(screen.getByText("Users")).toBeInTheDocument();
        expect(screen.getByText("Permissions")).toBeInTheDocument();
        expect(screen.getByText("Settings")).toBeInTheDocument();
    });

    it("shows neither Permissions nor Roles without PERMISSIONS_MANAGE, and both with it", () => {
        const { rerender } = renderSidebar(["POS_USE"]);
        expect(screen.queryByText("Permissions")).not.toBeInTheDocument();
        expect(screen.queryByText("Roles")).not.toBeInTheDocument();

        useAuth.mockReturnValue({ hasPermission: (perm) => perm === "PERMISSIONS_MANAGE" });
        rerender(
            <LanguageProvider>
                <Sidebar activePage="pos" onNavigate={vi.fn()} collapsed={false} onToggle={vi.fn()} />
            </LanguageProvider>
        );
        expect(screen.getByText("Permissions")).toBeInTheDocument();
        expect(screen.getByText("Roles")).toBeInTheDocument();
    });

    it("navigates when a visible item is clicked", async () => {
        const user = userEvent.setup();
        const onNavigate = vi.fn();
        renderSidebar(["CHECKOUT_USE"], { onNavigate });

        await user.click(screen.getByText("Billing"));
        expect(onNavigate).toHaveBeenCalledWith("billing");
    });
});
