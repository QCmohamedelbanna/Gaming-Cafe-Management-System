package com.cafe.ps.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeStatusControllerTest {

    @Test
    void exposesStableLauncherIdentityWithoutRuntimeSecrets() {
        Map<String, String> status = new RuntimeStatusController("1.0.0").status();

        assertThat(status)
                .containsEntry("application", "gaming-cafe")
                .containsEntry("name", "Gaming Cafe")
                .containsEntry("version", "1.0.0")
                .doesNotContainKey("clientSecret")
                .doesNotContainKey("accessToken");
    }
}
