package com.cafe.ps.controller;

import com.cafe.ps.dto.CloseShiftRequest;
import com.cafe.ps.dto.OpenShiftRequest;
import com.cafe.ps.dto.ShiftReportResponse;
import com.cafe.ps.dto.ShiftResponse;
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
@PreAuthorize("hasAuthority('PERMISSION_SHIFT_MANAGE')")
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
    @PreAuthorize("hasAuthority('PERMISSION_SHIFT_AUDIT')")
    public List<ShiftResponse> all() {
        return shiftService.all();
    }

    @GetMapping("/{id}/report")
    public ShiftReportResponse report(
            @PathVariable Long id,
            Authentication authentication
    ) {
        boolean canAuditAll = authentication.getAuthorities().stream()
                .anyMatch(granted -> "PERMISSION_SHIFT_AUDIT".equals(granted.getAuthority()));
        return shiftService.report(id, authentication.getName(), canAuditAll);
    }

}
