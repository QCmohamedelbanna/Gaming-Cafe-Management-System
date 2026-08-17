package com.cafe.ps.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateRuleRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 200) String description,
        Set<String> permissions
) {
}
