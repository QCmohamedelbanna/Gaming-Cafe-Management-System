package com.cafe.ps.controller;
import com.cafe.ps.dto.DeviceActiveRequest;
import com.cafe.ps.dto.DeviceRequest;
import com.cafe.ps.dto.DeviceResponse;
import com.cafe.ps.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService service;

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_DEVICES_MANAGE') and hasAuthority('PERMISSION_DESTRUCTIVE_OPERATIONS')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
