package com.cafe.ps.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Writes the locations an administrator needs when diagnosing a client PC. */
@Component
public class RuntimeStartupLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeStartupLogger.class);

    private final ApplicationPaths paths;

    public RuntimeStartupLogger(ApplicationPaths paths) {
        this.paths = paths;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        LOGGER.info(
                "Gaming Cafe ready: database={}, logs={}, backups={}",
                paths.databasePath(),
                paths.logDirectory(),
                paths.backupDirectory()
        );
    }
}
