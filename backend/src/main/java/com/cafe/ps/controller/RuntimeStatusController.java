package com.cafe.ps.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Small public identity endpoint used by the silent Windows launcher. */
@RestController
@RequestMapping("/api/system")
public class RuntimeStatusController {

    private final String version;

    public RuntimeStatusController(@Value("${app.version:1.0.0}") String version) {
        this.version = version;
    }

    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of(
                "application", "gaming-cafe",
                "name", "Gaming Cafe",
                "version", version
        );
    }
}
