package com.cafe.ps.dto;

import com.cafe.ps.entity.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String displayName,
        Role role,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
}
