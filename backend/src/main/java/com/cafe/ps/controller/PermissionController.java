package com.cafe.ps.controller;

import com.cafe.ps.dto.PermissionResponse;
import com.cafe.ps.dto.RolePermissionsRequest;
import com.cafe.ps.entity.Role;
import com.cafe.ps.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
@PreAuthorize("hasAuthority('PERMISSION_PERMISSIONS_MANAGE')")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public List<PermissionResponse> catalog() {
        return permissionService.catalog();
    }

    @PutMapping("/roles/{role}")
    public List<PermissionResponse> updateRole(
            @PathVariable Role role,
            @Valid @RequestBody RolePermissionsRequest request
    ) {
        return permissionService.updateRole(role, request);
    }
}
