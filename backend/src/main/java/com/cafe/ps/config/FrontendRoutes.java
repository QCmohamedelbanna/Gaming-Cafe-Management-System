package com.cafe.ps.config;

import java.util.ArrayList;
import java.util.List;

/** Canonical client-side paths shared by MVC fallback and Spring Security. */
public final class FrontendRoutes {

    private static final List<String> ROUTES = List.of(
            "/operations",
            "/pos",
            "/products",
            "/inventory",
            "/pricing",
            "/dashboard",
            "/reports",
            "/devices",
            "/users",
            "/settings",
            "/reservations",
            "/billing",
            "/shifts",
            "/permissions",
            "/rules"
    );

    private FrontendRoutes() {
    }

    public static List<String> routes() {
        return ROUTES;
    }

    public static String[] securityPermitPatterns() {
        List<String> patterns = new ArrayList<>();
        for (String route : ROUTES) {
            patterns.add(route);
            patterns.add(route + "/**");
        }
        return patterns.toArray(String[]::new);
    }
}
