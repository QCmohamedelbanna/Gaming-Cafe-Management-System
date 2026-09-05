package com.cafe.ps.controller;
import com.cafe.ps.dto.DeviceActiveRequest;
import com.cafe.ps.dto.DeviceRequest;
import com.cafe.ps.dto.DeviceResponse;
import com.cafe.ps.dto.DeviceControlConfigurationRequest;
import com.cafe.ps.dto.DeviceControlDiagnosticsResponse;
import com.cafe.ps.dto.DevicePowerStatusResponse;
import com.cafe.ps.dto.PowerCommandResult;
import com.cafe.ps.service.DeviceService;
import com.cafe.ps.service.DeviceControlLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService service;
    private final DeviceControlLifecycleService deviceControlService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_DEVICES_VIEW')")
    public List<DeviceResponse> all() {
        return service.getAll().stream().map(DeviceResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_DEVICES_MANAGE')")
    public DeviceResponse create(@Valid @RequestBody DeviceRequest request) {
        return DeviceResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_DEVICES_MANAGE')")
    public DeviceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody DeviceRequest request
    ) {
        return DeviceResponse.from(service.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAuthority('PERMISSION_DEVICES_MANAGE')")
    public DeviceResponse setActive(
            @PathVariable Long id,
            @Valid @RequestBody DeviceActiveRequest request
    ) {
        return DeviceResponse.from(service.setActive(id, request.active()));
    }

    @PatchMapping("/{id}/control")
    @PreAuthorize("hasAuthority('PERMISSION_DEVICES_MANAGE')")
    public DeviceResponse configureControl(
            @PathVariable Long id,
            @Valid @RequestBody DeviceControlConfigurationRequest request
    ) {
        return DeviceResponse.from(service.configureControl(id, request));
    }

    @GetMapping("/{id}/power")
    @PreAuthorize("hasAuthority('PERMISSION_DEVICES_VIEW')")
    public DevicePowerStatusResponse powerState(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return deviceControlService.powerStateNow(id, authentication.getName());
    }

    @PostMapping("/{id}/power/on")
    @PreAuthorize("hasAuthority('PERMISSION_DEVICES_MANAGE')")
    public PowerCommandResult powerOn(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return deviceControlService.powerOnNow(id, authentication.getName());
    }

    @PostMapping("/{id}/power/off")
    @PreAuthorize("hasAuthority('PERMISSION_DEVICES_MANAGE')")
    public PowerCommandResult powerOff(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return deviceControlService.powerOffNow(id, authentication.getName());
    }

    /** Development/setup diagnostic: returns codes, never raw provider JSON. */
    @GetMapping("/{id}/power/diagnostics")
    @PreAuthorize("hasAuthority('PERMISSION_DEVICES_MANAGE')")
    public DeviceControlDiagnosticsResponse diagnostics(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return deviceControlService.diagnostics(id, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_DEVICES_MANAGE') and hasAuthority('PERMISSION_DESTRUCTIVE_OPERATIONS')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
