package com.cafe.ps.entity;

import java.util.Set;

public enum Permission {
    OPERATIONS_USE(
            "Operations",
            "Start, stop, extend, and monitor gaming sessions.",
            "OPERATIONS",
            Set.of(Role.CASHIER, Role.MANAGER, Role.ADMIN)
    ),
    POS_USE(
            "Point of sale",
            "Create and manage cafe orders at the POS.",
            "SALES",
            Set.of(Role.CASHIER, Role.MANAGER, Role.ADMIN)
    ),
    CHECKOUT_USE(
            "Checkout",
            "Prepare bills and accept customer payments.",
            "SALES",
            Set.of(Role.CASHIER, Role.MANAGER, Role.ADMIN)
    ),
    BILL_REFUND(
            "Refund bills",
            "Refund completed bills with a recorded reason.",
            "SALES",
            Set.of(Role.ADMIN)
    ),
    SHIFT_MANAGE(
            "Cashier shifts",
            "Open, close, reconcile, and review cashier shifts.",
            "OPERATIONS",
            Set.of(Role.CASHIER, Role.MANAGER, Role.ADMIN)
    ),
    SHIFT_AUDIT(
            "Shift reports",
            "Review all cashiers' shifts and transaction reports.",
            "REPORTING",
            Set.of(Role.MANAGER, Role.ADMIN)
    ),
    DASHBOARD_VIEW(
            "Dashboard",
            "View management dashboard summaries.",
            "REPORTING",
            Set.of(Role.MANAGER, Role.ADMIN)
    ),
    PRODUCTS_VIEW(
            "Products - view",
            "Browse active products at the POS and in product lists.",
            "CATALOG",
            Set.of(Role.CASHIER, Role.MANAGER, Role.ADMIN)
    ),
    PRODUCTS_MANAGE(
            "Products - manage",
            "Create, edit, activate, and maintain products.",
            "CATALOG",
            Set.of(Role.MANAGER, Role.ADMIN)
    ),
    INVENTORY_VIEW(
            "Inventory - view",
            "View stock levels, categories, and movement history.",
            "INVENTORY",
            Set.of(Role.CASHIER, Role.MANAGER, Role.ADMIN)
    ),
    INVENTORY_MANAGE(
            "Inventory - manage",
            "Record purchases, adjustments, and waste.",
            "INVENTORY",
            Set.of(Role.MANAGER, Role.ADMIN)
    ),
    PRICING_VIEW(
            "Pricing - view",
            "View current session pricing rules.",
            "CATALOG",
            Set.of(Role.CASHIER, Role.MANAGER, Role.ADMIN)
    ),
    PRICING_MANAGE(
            "Pricing - manage",
            "Change session prices and pricing rules.",
            "CATALOG",
            Set.of(Role.MANAGER, Role.ADMIN)
    ),
    REPORTS_VIEW(
            "Reports",
            "View operational, sales, and shift reports.",
            "REPORTING",
            Set.of(Role.MANAGER, Role.ADMIN)
    ),
    DEVICES_VIEW(
            "Devices - view",
            "View gaming devices and availability.",
            "DEVICES",
            Set.of(Role.CASHIER, Role.MANAGER, Role.ADMIN)
    ),
    DEVICES_MANAGE(
            "Devices - manage",
            "Create, edit, activate, and maintain devices.",
            "DEVICES",
            Set.of(Role.ADMIN)
    ),
    BILLING_MANAGE(
            "Billing - manage",
            "Cancel pending bills and perform billing administration.",
            "SALES",
            Set.of(Role.MANAGER, Role.ADMIN)
    ),
    DISCOUNTS_MANAGE(
            "Discounts",
            "Apply discounts to open POS orders.",
            "SALES",
            Set.of(Role.MANAGER, Role.ADMIN)
    ),
    USERS_MANAGE(
            "Users",
            "Create, edit, disable, and manage user accounts.",
            "ACCESS CONTROL",
            Set.of(Role.ADMIN)
    ),
    PERMISSIONS_MANAGE(
            "Permissions",
            "Assign permissions to system roles.",
            "ACCESS CONTROL",
            Set.of(Role.ADMIN)
    ),
    SETTINGS_MANAGE(
            "Settings",
            "Manage application and system settings.",
            "ADMINISTRATION",
            Set.of(Role.ADMIN)
    ),
    DESTRUCTIVE_OPERATIONS(
            "Destructive operations",
            "Delete records and remove devices or products.",
            "ADMINISTRATION",
            Set.of(Role.ADMIN)
    );

    private final String label;
    private final String description;
    private final String category;
    private final Set<Role> defaultRoles;

    Permission(String label, String description, String category, Set<Role> defaultRoles) {
        this.label = label;
        this.description = description;
        this.category = category;
        this.defaultRoles = defaultRoles;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public String category() {
        return category;
    }

    public Set<Role> defaultRoles() {
        return defaultRoles;
    }
}
