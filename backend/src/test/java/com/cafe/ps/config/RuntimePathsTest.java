package com.cafe.ps.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimePathsTest {

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
}
