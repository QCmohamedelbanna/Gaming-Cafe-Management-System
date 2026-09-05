package com.cafe.ps.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "tuya")
public class TuyaProperties {
    private boolean enabled = false;
    private URI endpoint = URI.create("https://openapi.tuyaus.com");
    private String clientId = "";
    private String clientSecret = "";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration requestTimeout = Duration.ofSeconds(5);
    private int maxAttempts = 2;
}
