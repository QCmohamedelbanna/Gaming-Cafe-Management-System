package com.cafe.ps.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/** Fails startup before any request can attempt a misconfigured Tuya call. */
@Component
public class TuyaConfigurationValidator {

    private final DeviceControlProperties deviceControlProperties;
    private final TuyaProperties tuyaProperties;

    public TuyaConfigurationValidator(
            DeviceControlProperties deviceControlProperties,
            TuyaProperties tuyaProperties
    ) {
        this.deviceControlProperties = deviceControlProperties;
        this.tuyaProperties = tuyaProperties;
    }

    @PostConstruct
    void validate() {
        if (!"tuya".equalsIgnoreCase(deviceControlProperties.getMode())
                || !tuyaProperties.isEnabled()) {
            return;
        }

        if (!configured(tuyaProperties.getClientId())
                || !configured(tuyaProperties.getClientSecret())) {
            throw new IllegalStateException(
                    "Tuya Cloud is enabled in tuya mode, but "
                            + "TUYA_CLIENT_ID and TUYA_CLIENT_SECRET must both be configured"
            );
        }
    }

    private static boolean configured(String value) {
        return value != null && !value.isBlank();
    }
}
