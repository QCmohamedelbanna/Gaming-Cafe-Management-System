package com.cafe.ps.dto;

import com.cafe.ps.entity.Role;

import java.util.Set;

public record PermissionResponse(
        String code,
        String label,
        String description,
        String category,
        Set<Role> roles
) {
}
