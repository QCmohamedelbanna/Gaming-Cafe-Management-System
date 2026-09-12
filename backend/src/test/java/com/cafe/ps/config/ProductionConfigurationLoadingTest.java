package com.cafe.ps.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionConfigurationLoadingTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void productionLoadsExternalConfigAndCommandLineOverridesIt() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("runtime");
        Path config = dataDirectory.resolve("config/gaming-cafe.properties");
        Files.createDirectories(config.getParent());
        Files.writeString(
                config,
                "device-control.mode=tuya\n"
                        + "tuya.enabled=true\n"
                        + "tuya.endpoint=https://openapi.tuyaeu.com\n"
                        + "tuya.client-id=external-client-id\n"
                        + "tuya.client-secret=fake-test-secret\n"
                        + "tuya.connect-timeout=2s\n"
                        + "tuya.request-timeout=5s\n"
                        + "tuya.max-attempts=2\n",
                StandardCharsets.UTF_8
        );

        SpringApplication application = new SpringApplication(ProbeConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext context = application.run(
                "--app.data-dir=" + dataDirectory,
                "--GAMING_CAFE_CONFIG_FILE=" + config.toUri(),
                "--tuya.client-id=command-line-client-id"
        )) {
            DeviceControlProperties deviceControl = context.getBean(DeviceControlProperties.class);
            TuyaProperties tuya = context.getBean(TuyaProperties.class);

            assertThat(deviceControl.getMode()).isEqualTo("tuya");
            assertThat(tuya.isEnabled()).isTrue();
            assertThat(tuya.getClientId()).isEqualTo("command-line-client-id");
            assertThat(tuya.getClientSecret()).isEqualTo("fake-test-secret");
            assertThat(tuya.getEndpoint().toString())
                    .isEqualTo("https://openapi.tuyaeu.com");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({DeviceControlProperties.class, TuyaProperties.class})
    static class ProbeConfiguration {
    }
}
