package com.cafe.ps.dto;

import com.cafe.ps.entity.Role;

import java.util.Set;

public record RuleResponse(
        Long id,
        String name,
        String description,
        Boolean systemRule,
        Role systemRole,
        long userCount,
        Set<String> permissions
) {
}
