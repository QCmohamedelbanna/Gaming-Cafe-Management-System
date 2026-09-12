package com.cafe.ps.config;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

/**
 * Centralizes all filesystem locations used by the installed application.
 * The launcher and Spring's environment setup use the same resolver so they
 * cannot accidentally acquire different locks or open different databases.
 */
public final class RuntimePaths {

    public static final String APPLICATION_ID = "GamingCafe";
    public static final String APPLICATION_NAME = "Gaming Cafe";
    public static final int DEFAULT_PORT = 8080;
    public static final String PRODUCTION_CONFIG_FILE_NAME = "gaming-cafe.properties";
    public static final String PRODUCTION_CONFIG_TEMPLATE_FILE_NAME =
            PRODUCTION_CONFIG_FILE_NAME + ".example";

    private static final String CONFIG_FILE_PROPERTY = "GAMING_CAFE_CONFIG_FILE";
    private static final String CONFIG_PORT_PROPERTY = "server.port";

    private RuntimePaths() {
    }

    public static Path defaultDataDirectory() {
        String configured = configuredDataDirectory();
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

    /**
     * Returns a writable default for a client runtime. Windows prefers the
     * shared ProgramData location, but a standard user may run on a machine
     * where the installer could not grant write access to that directory.
     * Explicit app.data-dir/GAMING_CAFE_DATA_DIR values are always respected
     * and fail clearly if they cannot be used.
     */
    public static Path resolveDataDirectory() {
        if (configuredDataDirectory() != null) {
            return defaultDataDirectory();
        }

        Path preferred = defaultDataDirectory();
        if (!isWindows() || ensureWritable(preferred)) {
            return preferred;
        }

        String localAppData = firstNonBlank(
                System.getenv("LocalAppData"),
                System.getenv("LOCALAPPDATA")
        );
        if (localAppData != null) {
            Path perUser = Path.of(localAppData, APPLICATION_ID);
            if (ensureWritable(perUser)) return perUser;
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
        return applicationPort(defaultDataDirectory());
    }

    /**
     * Resolves the launcher port using the same precedence as Spring Boot:
     * command-line/system properties, process environment, external client
     * configuration, then the packaged default.
     */
    public static int applicationPort(Path dataDirectory, String... commandLineArgs) {
        String commandLinePort = commandLineProperty(commandLineArgs, CONFIG_PORT_PROPERTY);
        String configured = firstNonBlank(
                commandLinePort,
                System.getProperty("server.port"),
                System.getProperty("gaming.cafe.port"),
                System.getenv("SERVER_PORT"),
                System.getenv("GAMING_CAFE_PORT")
        );
        if (configured != null) return parsePort(configured);

        Properties external = readExternalConfiguration(dataDirectory, commandLineArgs);
        return parsePort(firstNonBlank(
                external.getProperty(CONFIG_PORT_PROPERTY),
                external.getProperty("gaming.cafe.port")
        ));
    }

    public static Path configDirectory(Path dataDirectory) {
        return dataDirectory.resolve("config").toAbsolutePath().normalize();
    }

    public static Path productionConfigPath(Path dataDirectory, String... commandLineArgs) {
        String configured = firstNonBlank(
                commandLineProperty(commandLineArgs, CONFIG_FILE_PROPERTY),
                System.getProperty(CONFIG_FILE_PROPERTY),
                System.getenv(CONFIG_FILE_PROPERTY)
        );
        if (configured != null) {
            return configuredPath(configured);
        }
        return configDirectory(dataDirectory)
                .resolve(PRODUCTION_CONFIG_FILE_NAME)
                .toAbsolutePath()
                .normalize();
    }

    public static Path productionConfigTemplatePath(Path dataDirectory) {
        return configDirectory(dataDirectory)
                .resolve(PRODUCTION_CONFIG_TEMPLATE_FILE_NAME)
                .toAbsolutePath()
                .normalize();
    }

    /**
     * Creates a non-secret first-run guide without replacing a client-managed
     * configuration file. The application never writes credentials here.
     */
    public static void ensureProductionConfigTemplate(Path dataDirectory) throws IOException {
        Path configDirectory = configDirectory(dataDirectory);
        Files.createDirectories(configDirectory);
        Path template = productionConfigTemplatePath(dataDirectory);
        if (Files.exists(template)) return;

        Files.writeString(
                template,
                productionConfigTemplate(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
    }

    public static String productionConfigTemplate() {
        return "# Gaming Cafe production configuration\n"
                + "# Copy this file to gaming-cafe.properties and replace values privately.\n"
                + "# This file is read by Spring Boot and is never served to the browser.\n"
                + "\n"
                + "# Keep mock mode until Tuya has been configured and tested.\n"
                + "device-control.mode=mock\n"
                + "tuya.enabled=false\n"
                + "tuya.endpoint=https://openapi.tuyaeu.com\n"
                + "tuya.client-id=\n"
                + "tuya.client-secret=\n"
                + "tuya.connect-timeout=2s\n"
                + "tuya.request-timeout=5s\n"
                + "tuya.max-attempts=2\n";
    }

    public static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String configuredDataDirectory() {
        return firstNonBlank(
                System.getProperty("app.data-dir"),
                System.getProperty("gaming.cafe.data-dir"),
                System.getenv("GAMING_CAFE_DATA_DIR")
        );
    }

    private static Properties readExternalConfiguration(
            Path dataDirectory,
            String... commandLineArgs
    ) {
        Properties properties = new Properties();
        Path configPath = productionConfigPath(dataDirectory, commandLineArgs);
        if (!Files.isRegularFile(configPath)) return properties;
        try (var reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException | IllegalArgumentException ignored) {
            // Spring Boot remains the source of truth and will report any
            // malformed required configuration during normal startup.
        }
        return properties;
    }

    private static String commandLineProperty(String[] args, String propertyName) {
        if (args == null) return null;
        String prefix = "--" + propertyName + "=";
        for (String argument : args) {
            if (argument != null && argument.startsWith(prefix)) {
                return argument.substring(prefix.length());
            }
        }
        return null;
    }

    private static int parsePort(String configured) {
        if (configured == null) return DEFAULT_PORT;
        try {
            int port = Integer.parseInt(configured);
            return port >= 1 && port <= 65535 ? port : DEFAULT_PORT;
        } catch (NumberFormatException ignored) {
            return DEFAULT_PORT;
        }
    }

    private static Path configuredPath(String configured) {
        if (configured.startsWith("file:")) {
            try {
                return Path.of(URI.create(configured)).toAbsolutePath().normalize();
            } catch (IllegalArgumentException ignored) {
                // Fall through and let the normal path parser provide the
                // clearest error for a malformed override.
            }
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    private static boolean ensureWritable(Path directory) {
        try {
            Files.createDirectories(directory);
            return Files.isDirectory(directory) && Files.isWritable(directory);
        } catch (IOException | SecurityException ignored) {
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(java.util.Locale.ROOT)
                .contains("win");
    }
}
