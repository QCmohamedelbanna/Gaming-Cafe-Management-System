package com.cafe.ps.controller;

import com.cafe.ps.dto.SettingsResponse;
import com.cafe.ps.dto.UpdateSettingsRequest;
import com.cafe.ps.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERMISSION_SETTINGS_MANAGE')")
public class SettingsController {

    private final SettingsService service;

    @GetMapping
    public SettingsResponse get() {
        return SettingsResponse.from(service.get());
    }

    @PutMapping
    public SettingsResponse update(@Valid @RequestBody UpdateSettingsRequest request) {
        return SettingsResponse.from(service.update(request));
    }
}
