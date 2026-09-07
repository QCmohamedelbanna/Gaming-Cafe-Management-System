package com.cafe.ps.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TuyaConfigurationValidatorTest {

    @Test
    void rejectsEnabledTuyaModeWhenCredentialsAreMissing() {
        DeviceControlProperties deviceControl = new DeviceControlProperties();
        deviceControl.setMode("tuya");

        TuyaProperties tuya = new TuyaProperties();
        tuya.setEnabled(true);

        assertThatThrownBy(() -> new TuyaConfigurationValidator(deviceControl, tuya).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tuya Cloud is enabled in tuya mode, but "
                        + "TUYA_CLIENT_ID and TUYA_CLIENT_SECRET must both be configured");
    }

    @Test
    void permitsMockModeWithoutTuyaCredentials() {
        DeviceControlProperties deviceControl = new DeviceControlProperties();

        TuyaProperties tuya = new TuyaProperties();
        tuya.setEnabled(false);

        assertThatCode(() -> new TuyaConfigurationValidator(deviceControl, tuya).validate())
                .doesNotThrowAnyException();
    }

    @Test
    void permitsEnabledTuyaModeWhenBothCredentialsAreConfigured() {
        DeviceControlProperties deviceControl = new DeviceControlProperties();
        deviceControl.setMode("tuya");

        TuyaProperties tuya = new TuyaProperties();
        tuya.setEnabled(true);
        tuya.setClientId("configured-id");
        tuya.setClientSecret("configured-secret");

        assertThatCode(() -> new TuyaConfigurationValidator(deviceControl, tuya).validate())
                .doesNotThrowAnyException();
    }
}
