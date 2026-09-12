package com.cafe.ps.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimePathsTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void databasePathUsesThePersistentDataLayoutByDefault() {
        Path dataDirectory = Path.of("target", "runtime-path-test");

        assertThat(RuntimePaths.databasePath(dataDirectory))
                .isEqualTo(dataDirectory.toAbsolutePath().normalize()
                        .resolve("data")
                        .resolve("gaming-cafe.db"));
    }

    @Test
    void firstNonBlankIgnoresEmptyOverrides() {
        assertThat(RuntimePaths.firstNonBlank("", "  ", "configured"))
                .isEqualTo("configured");
    }

    @Test
    void productionConfigurationUsesThePersistentConfigLayout() {
        Path dataDirectory = temporaryDirectory.resolve("GamingCafe");

        assertThat(RuntimePaths.configDirectory(dataDirectory))
                .isEqualTo(dataDirectory.resolve("config").toAbsolutePath().normalize());
        assertThat(RuntimePaths.productionConfigPath(dataDirectory))
                .isEqualTo(dataDirectory.resolve("config/gaming-cafe.properties")
                        .toAbsolutePath().normalize());
        assertThat(RuntimePaths.productionConfigTemplatePath(dataDirectory))
                .isEqualTo(dataDirectory.resolve("config/gaming-cafe.properties.example")
                        .toAbsolutePath().normalize());
    }

    @Test
    void productionTemplateIsCreatedWithoutCredentials() throws IOException {
        Path dataDirectory = temporaryDirectory.resolve("GamingCafe");

        RuntimePaths.ensureProductionConfigTemplate(dataDirectory);
        Path template = RuntimePaths.productionConfigTemplatePath(dataDirectory);
        assertThat(Files.readString(template, StandardCharsets.UTF_8))
                .contains("device-control.mode=mock")
                .contains("tuya.enabled=false")
                .contains("tuya.client-secret=")
                .doesNotContain("<real")
                .doesNotContain("access_token");

        String original = Files.readString(template, StandardCharsets.UTF_8);
        RuntimePaths.ensureProductionConfigTemplate(dataDirectory);
        assertThat(Files.readString(template, StandardCharsets.UTF_8))
                .isEqualTo(original);
    }

    @Test
    void launcherUsesExternalPortWhenNoHigherPriorityOverrideExists() throws IOException {
        Path dataDirectory = temporaryDirectory.resolve("GamingCafe");
        Path config = RuntimePaths.productionConfigPath(dataDirectory);
        Files.createDirectories(config.getParent());
        Files.writeString(config, "server.port=9191\n", StandardCharsets.UTF_8);

        String previousConfig = System.getProperty("GAMING_CAFE_CONFIG_FILE");
        String previousServerPort = System.getProperty("server.port");
        try {
            System.setProperty("GAMING_CAFE_CONFIG_FILE", config.toString());
            System.clearProperty("server.port");

            assertThat(RuntimePaths.applicationPort(dataDirectory)).isEqualTo(9191);
        } finally {
            restoreProperty("GAMING_CAFE_CONFIG_FILE", previousConfig);
            restoreProperty("server.port", previousServerPort);
        }
    }

    @Test
    void commandLinePortTakesPrecedenceOverExternalConfiguration() throws IOException {
        Path dataDirectory = temporaryDirectory.resolve("GamingCafe");
        Path config = RuntimePaths.productionConfigPath(dataDirectory);
        Files.createDirectories(config.getParent());
        Files.writeString(config, "server.port=9191\n", StandardCharsets.UTF_8);

        String previousConfig = System.getProperty("GAMING_CAFE_CONFIG_FILE");
        String previousServerPort = System.getProperty("server.port");
        try {
            System.setProperty("GAMING_CAFE_CONFIG_FILE", config.toString());
            System.clearProperty("server.port");

            assertThat(RuntimePaths.applicationPort(dataDirectory, "--server.port=9292"))
                    .isEqualTo(9292);
        } finally {
            restoreProperty("GAMING_CAFE_CONFIG_FILE", previousConfig);
            restoreProperty("server.port", previousServerPort);
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) System.clearProperty(name);
        else System.setProperty(name, value);
    }
}
