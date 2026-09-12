package com.cafe.ps.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves and creates runtime directories before the datasource and Logback
 * are initialized. This is what makes a fresh installation work when
 * C:\\ProgramData\\GamingCafe does not exist yet.
 */
public final class ApplicationPathsEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE = "gamingCafeRuntimePaths";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application
    ) {
        boolean development = isDevelopmentProfile(environment);
        Path defaultDataDirectory = development
                ? Path.of(".")
                : RuntimePaths.resolveDataDirectory();

        Path dataDirectory = pathProperty(
                environment,
                "app.data-dir",
                defaultDataDirectory
        );
        Path databasePath = pathProperty(
                environment,
                "app.database-path",
                dataDirectory.resolve("data").resolve("gaming-cafe.db")
        );
        Path logDirectory = pathProperty(
                environment,
                "app.log-dir",
                dataDirectory.resolve("logs")
        );
        Path backupDirectory = pathProperty(
                environment,
                "app.backup-dir",
                dataDirectory.resolve("backup")
        );
        Path configDirectory = RuntimePaths.configDirectory(dataDirectory);
        Path lockFile = dataDirectory.resolve("gaming-cafe.lock");

        try {
            createDirectories(dataDirectory);
            createDirectories(databasePath.toAbsolutePath().normalize().getParent());
            createDirectories(logDirectory);
            createDirectories(backupDirectory);
            createDirectories(configDirectory);
            if (isProductionProfile(environment)) {
                RuntimePaths.ensureProductionConfigTemplate(dataDirectory);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to create Gaming Cafe runtime directories under "
                            + dataDirectory.toAbsolutePath(),
                    exception
            );
        }

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("app.data-dir", dataDirectory.toAbsolutePath().normalize().toString());
        values.put("app.database-path", databasePath.toAbsolutePath().normalize().toString());
        values.put("app.log-dir", logDirectory.toAbsolutePath().normalize().toString());
        values.put("app.backup-dir", backupDirectory.toAbsolutePath().normalize().toString());
        values.put("app.lock-file", lockFile.toAbsolutePath().normalize().toString());

        environment.getPropertySources().addFirst(
                new MapPropertySource(PROPERTY_SOURCE, values)
        );
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private static Path pathProperty(
            ConfigurableEnvironment environment,
            String propertyName,
            Path fallback
    ) {
        String configured = RuntimePaths.firstNonBlank(
                environment.getProperty(propertyName)
        );
        return configured == null ? fallback : Path.of(configured);
    }

    private static void createDirectories(Path directory) throws IOException {
        if (directory != null) {
            Files.createDirectories(directory);
        }
    }

    private static boolean isDevelopmentProfile(ConfigurableEnvironment environment) {
        for (String profile : environment.getActiveProfiles()) {
            if ("dev".equalsIgnoreCase(profile)) return true;
        }
        return false;
    }

    private static boolean isProductionProfile(ConfigurableEnvironment environment) {
        return environment.acceptsProfiles(Profiles.of("prod"));
    }
}
