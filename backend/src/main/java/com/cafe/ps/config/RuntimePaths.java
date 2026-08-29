package com.cafe.ps.config;

import java.nio.file.Path;

/**
 * Centralizes all filesystem locations used by the installed application.
 * The launcher and Spring's environment setup use the same resolver so they
 * cannot accidentally acquire different locks or open different databases.
 */
public final class RuntimePaths {

    public static final String APPLICATION_ID = "GamingCafe";
    public static final String APPLICATION_NAME = "Gaming Cafe";
    public static final int DEFAULT_PORT = 8080;

    private RuntimePaths() {
    }

    public static Path defaultDataDirectory() {
        String configured = firstNonBlank(
                System.getProperty("app.data-dir"),
                System.getProperty("gaming.cafe.data-dir"),
                System.getenv("GAMING_CAFE_DATA_DIR")
        );
        if (configured != null) {
            return Path.of(configured);
        }

        String programData = firstNonBlank(
                System.getenv("ProgramData"),
                System.getenv("PROGRAMDATA")
        );
        if (isWindows() && programData != null) {
            return Path.of(programData, APPLICATION_ID);
        }

        return Path.of(System.getProperty("user.home"), ".gaming-cafe");
    }

    public static Path databasePath(Path dataDirectory) {
        String configured = firstNonBlank(
                System.getProperty("app.database-path"),
                System.getProperty("gaming.cafe.db-path"),
                System.getenv("GAMING_CAFE_DB_PATH")
        );
        Path path = configured == null
                ? dataDirectory.resolve("data").resolve("gaming-cafe.db")
                : Path.of(configured);
        return path.toAbsolutePath().normalize();
    }

    public static int applicationPort() {
        String configured = firstNonBlank(
                System.getProperty("server.port"),
                System.getProperty("gaming.cafe.port"),
                System.getenv("SERVER_PORT"),
                System.getenv("GAMING_CAFE_PORT")
        );
        if (configured == null) return DEFAULT_PORT;
        try {
            return Integer.parseInt(configured);
        } catch (NumberFormatException ignored) {
            return DEFAULT_PORT;
        }
    }

    public static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(java.util.Locale.ROOT)
                .contains("win");
    }
}
