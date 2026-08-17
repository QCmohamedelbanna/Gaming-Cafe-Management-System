package com.cafe.ps.dto;

import com.cafe.ps.entity.Role;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String displayName,
        Role role,
        Long ruleId,
        String ruleName,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        Set<String> permissions
) {
}
