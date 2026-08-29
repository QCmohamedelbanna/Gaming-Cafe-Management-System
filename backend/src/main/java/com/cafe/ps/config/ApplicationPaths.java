package com.cafe.ps.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolved filesystem paths exposed to runtime services. */
@Component
public class ApplicationPaths {

    private final Path dataDirectory;
    private final Path databasePath;
    private final Path logDirectory;
    private final Path backupDirectory;
    private final Path lockFile;

    public ApplicationPaths(Environment environment) {
        this.dataDirectory = Path.of(environment.getRequiredProperty("app.data-dir"));
        this.databasePath = Path.of(environment.getRequiredProperty("app.database-path"));
        this.logDirectory = Path.of(environment.getRequiredProperty("app.log-dir"));
        this.backupDirectory = Path.of(environment.getRequiredProperty("app.backup-dir"));
        this.lockFile = Path.of(environment.getRequiredProperty("app.lock-file"));
    }

    @PostConstruct
    void verifyDirectories() {
        try {
            Files.createDirectories(dataDirectory);
            Files.createDirectories(databasePath.toAbsolutePath().normalize().getParent());
            Files.createDirectories(logDirectory);
            Files.createDirectories(backupDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to access Gaming Cafe runtime directories under "
                            + dataDirectory.toAbsolutePath(),
                    exception
            );
        }
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    public Path databasePath() {
        return databasePath;
    }

    public Path logDirectory() {
        return logDirectory;
    }

    public Path backupDirectory() {
        return backupDirectory;
    }

    public Path lockFile() {
        return lockFile;
    }
}
