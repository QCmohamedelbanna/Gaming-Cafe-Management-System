package com.cafe.ps.controller;
import com.cafe.ps.dto.DeviceActiveRequest;
import com.cafe.ps.dto.DeviceRequest;
import com.cafe.ps.entity.Device;
import com.cafe.ps.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class DeviceController {
    private final DeviceService service;

    @GetMapping
    public List<Device> all() {
        return service.getAll();
    }

    @PostMapping
    public Device create(@Valid @RequestBody DeviceRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Device update(
            @PathVariable Long id,
            @Valid @RequestBody DeviceRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public Device setActive(
            @PathVariable Long id,
            @Valid @RequestBody DeviceActiveRequest request
    ) {
        return service.setActive(id, request.active());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
