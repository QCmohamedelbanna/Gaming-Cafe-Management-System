package com.cafe.ps.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Writes safe runtime locations and configuration diagnostics for a client PC. */
@Component
public class RuntimeStartupLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeStartupLogger.class);

    private final ApplicationPaths paths;
    private final DeviceControlProperties deviceControlProperties;
    private final TuyaProperties tuyaProperties;

    public RuntimeStartupLogger(
            ApplicationPaths paths,
            DeviceControlProperties deviceControlProperties,
            TuyaProperties tuyaProperties
    ) {
        this.paths = paths;
        this.deviceControlProperties = deviceControlProperties;
        this.tuyaProperties = tuyaProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        LOGGER.info(
                "Gaming Cafe ready: database={}, logs={}, backups={}",
                paths.databasePath(),
                paths.logDirectory(),
                paths.backupDirectory()
        );
        LOGGER.info(
                "Gaming Cafe configuration: device control mode={}, Tuya enabled={}, "
                        + "Tuya endpoint={}, Tuya client id configured={}, "
                        + "Tuya client secret configured={}",
                deviceControlProperties.getMode(),
                tuyaProperties.isEnabled(),
                tuyaProperties.getEndpoint(),
                configured(tuyaProperties.getClientId()),
                configured(tuyaProperties.getClientSecret())
        );
    }

    private static boolean configured(String value) {
        return value != null && !value.isBlank();
    }
}
