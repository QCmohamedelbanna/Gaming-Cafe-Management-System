package com.cafe.ps.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.StandardOpenOption;

/**
 * Protects direct java -jar/mvn launches. The packaged launcher manages this
 * lock itself while it owns the Spring context, so it sets
 * app.launcher-managed=true and this bean stays disabled in that process.
 */
@Component
@ConditionalOnProperty(
        name = "app.launcher-managed",
        havingValue = "false",
        matchIfMissing = true
)
public class SingleInstanceGuard implements AutoCloseable {

    private final ApplicationPaths paths;
    private FileChannel channel;
    private FileLock lock;

    public SingleInstanceGuard(ApplicationPaths paths) {
        this.paths = paths;
    }

    @PostConstruct
    void acquire() {
        try {
            channel = FileChannel.open(
                    paths.lockFile(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
            );
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException ignored) {
                lock = null;
            }
            if (lock == null) {
                close();
                throw new IllegalStateException(
                        "Gaming Cafe is already running for this data directory"
                );
            }
        } catch (IOException exception) {
            close();
            throw new IllegalStateException(
                    "Unable to acquire the Gaming Cafe single-instance lock at "
                            + paths.lockFile(),
                    exception
            );
        }
    }

    @Override
    @PreDestroy
    public void close() {
        try {
            if (lock != null && lock.isValid()) lock.release();
        } catch (IOException ignored) {
            // The operating system releases the lock when the channel closes.
        } finally {
            lock = null;
            try {
                if (channel != null && channel.isOpen()) channel.close();
            } catch (IOException ignored) {
                // Best-effort cleanup during application shutdown.
            } finally {
                channel = null;
            }
        }
    }
}
