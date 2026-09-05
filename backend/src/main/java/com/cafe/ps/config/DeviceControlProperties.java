package com.cafe.ps.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "device-control")
public class DeviceControlProperties {
    /** mock, tuya, or none */
    private String mode = "mock";
    private boolean mockFailure = false;
    private String mockFailureMessage = "Mock device-control failure";
}
