package com.cafe.ps.controller;

import com.cafe.ps.dto.CloseShiftRequest;
import com.cafe.ps.dto.OpenShiftRequest;
import com.cafe.ps.dto.ShiftReportResponse;
import com.cafe.ps.dto.ShiftResponse;
import com.cafe.ps.entity.Role;
import com.cafe.ps.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
@PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'ADMIN')")
public class ShiftController {

    private final ShiftService shiftService;

    @GetMapping("/current")
    public ResponseEntity<ShiftResponse> current(Authentication authentication) {
        return shiftService.current(authentication.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/mine")
    public List<ShiftResponse> mine(Authentication authentication) {
        return shiftService.mine(authentication.getName());
    }

    @PostMapping
    public ShiftResponse open(
            @Valid @RequestBody OpenShiftRequest request,
            Authentication authentication
    ) {
        return shiftService.open(authentication.getName(), request);
    }

    @PostMapping("/close")
    public ShiftResponse close(
            @Valid @RequestBody CloseShiftRequest request,
            Authentication authentication
    ) {
        return shiftService.close(authentication.getName(), request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<ShiftResponse> all() {
        return shiftService.all();
    }

    @GetMapping("/{id}/report")
    public ShiftReportResponse report(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return shiftService.report(id, authentication.getName(), role(authentication));
    }

    private static Role role(Authentication authentication) {
        String authority = authentication.getAuthorities().stream()
                .map(granted -> granted.getAuthority())
                .filter(value -> value.startsWith("ROLE_"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Authenticated user has no role"));
        return Role.valueOf(authority.substring("ROLE_".length()));
    }
}
