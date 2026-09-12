package com.cafe.ps.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationPathsEnvironmentPostProcessorTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void productionCreatesPersistentDirectoriesAndSafeTemplate() {
        Path dataDirectory = temporaryDirectory.resolve("production-data");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.data-dir", dataDirectory.toString());
        environment.setActiveProfiles("prod");

        new ApplicationPathsEnvironmentPostProcessor().postProcessEnvironment(
                environment,
                new SpringApplication(Object.class)
        );

        assertThat(Files.isDirectory(dataDirectory.resolve("data"))).isTrue();
        assertThat(Files.isDirectory(dataDirectory.resolve("logs"))).isTrue();
        assertThat(Files.isDirectory(dataDirectory.resolve("backup"))).isTrue();
        assertThat(Files.isDirectory(dataDirectory.resolve("config"))).isTrue();
        assertThat(Files.isRegularFile(
                RuntimePaths.productionConfigTemplatePath(dataDirectory)
        )).isTrue();
        assertThat(environment.getProperty("app.database-path"))
                .isEqualTo(dataDirectory.resolve("data/gaming-cafe.db")
                        .toAbsolutePath().normalize().toString());
    }

    @Test
    void developmentDoesNotCreateProductionTemplate() {
        Path dataDirectory = temporaryDirectory.resolve("development-data");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.data-dir", dataDirectory.toString());
        environment.setActiveProfiles("dev");

        new ApplicationPathsEnvironmentPostProcessor().postProcessEnvironment(
                environment,
                new SpringApplication(Object.class)
        );

        assertThat(Files.isDirectory(dataDirectory.resolve("config"))).isTrue();
        assertThat(Files.exists(
                RuntimePaths.productionConfigTemplatePath(dataDirectory)
        )).isFalse();
    }

    @Test
    void higherPriorityRuntimePathOverrideWinsOverExternalConfiguration() {
        Path externalDataDirectory = temporaryDirectory.resolve("external-data");
        Path commandLineDataDirectory = temporaryDirectory.resolve("command-line-data");
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource(
                "externalProductionConfiguration",
                Map.of("app.data-dir", externalDataDirectory.toString())
        ));
        environment.getPropertySources().addFirst(new MapPropertySource(
                "commandLine",
                Map.of("app.data-dir", commandLineDataDirectory.toString())
        ));
        environment.setActiveProfiles("prod");

        new ApplicationPathsEnvironmentPostProcessor().postProcessEnvironment(
                environment,
                new SpringApplication(Object.class)
        );

        assertThat(environment.getProperty("app.data-dir"))
                .isEqualTo(commandLineDataDirectory.toAbsolutePath().normalize().toString());
        assertThat(Files.isDirectory(commandLineDataDirectory.resolve("config"))).isTrue();
        assertThat(Files.exists(externalDataDirectory)).isFalse();
    }
}
