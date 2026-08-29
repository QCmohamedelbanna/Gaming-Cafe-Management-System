package com.cafe.ps.service;

import com.cafe.ps.config.ApplicationPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

import org.sqlite.SQLiteConnection;

/**
 * Creates a consistent SQLite backup through SQLite's online backup API.
 * It never copies the live database file byte-for-byte while it may be in a
 * write transaction.
 */
@Service
public class DatabaseBackupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBackupService.class);
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    private final ApplicationPaths paths;
    private final DataSource dataSource;
    private final ReentrantLock backupLock = new ReentrantLock(true);

    public DatabaseBackupService(ApplicationPaths paths, DataSource dataSource) {
        this.paths = paths;
        this.dataSource = dataSource;
    }

    public BackupResult createBackup() {
        backupLock.lock();
        Path temporaryPath = null;
        try {
            Files.createDirectories(paths.backupDirectory());
            Path destination = nextDestination();
            temporaryPath = destination.resolveSibling(
                    "." + destination.getFileName() + ".tmp"
            );
            Files.deleteIfExists(temporaryPath);

            try (Connection connection = dataSource.getConnection()) {
                SQLiteConnection sqliteConnection = connection.unwrap(SQLiteConnection.class);
                int result = sqliteConnection.getDatabase().backup(
                        "main",
                        temporaryPath.toString(),
                        null
                );
                if (result != 0) {
                    throw new SQLException("SQLite online backup returned code " + result);
                }
            }

            long size = Files.size(temporaryPath);
            if (size == 0) {
                throw new IOException("SQLite online backup produced an empty file");
            }
            moveIntoPlace(temporaryPath, destination);
            temporaryPath = null;

            LOGGER.info("SQLite backup created: {} ({} bytes)", destination, size);
            return new BackupResult(destination.getFileName().toString(), destination, size);
        } catch (Exception exception) {
            LOGGER.error("SQLite backup failed for {}", paths.databasePath(), exception);
            throw new IllegalStateException("Database backup failed", exception);
        } finally {
            if (temporaryPath != null) {
                try {
                    Files.deleteIfExists(temporaryPath);
                } catch (IOException cleanupException) {
                    LOGGER.warn("Unable to remove incomplete backup {}", temporaryPath, cleanupException);
                }
            }
            backupLock.unlock();
        }
    }

    private Path nextDestination() {
        String baseName = "gaming-cafe-"
                + FILE_TIMESTAMP.format(LocalDateTime.now())
                + ".db";
        Path candidate = paths.backupDirectory().resolve(baseName);
        int suffix = 1;
        while (Files.exists(candidate)) {
            candidate = paths.backupDirectory().resolve(
                    baseName.substring(0, baseName.length() - 3)
                            + "-" + suffix++ + ".db"
            );
        }
        return candidate;
    }

    private static void moveIntoPlace(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination);
        }
    }

    public record BackupResult(String fileName, Path path, long sizeBytes) {
    }
}
