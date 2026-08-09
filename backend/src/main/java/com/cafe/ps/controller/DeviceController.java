package com.cafe.ps.controller;
import com.cafe.ps.entity.Device;
import com.cafe.ps.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")

public class DeviceController {
    private final DeviceRepository repo;
    @GetMapping public List<Device> all() { return repo.findAll(); }
}
