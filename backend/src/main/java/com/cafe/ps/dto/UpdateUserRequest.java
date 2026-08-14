package com.cafe.ps.dto;

import com.cafe.ps.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 100) String displayName,
        @NotNull Role role,
        @NotNull Boolean active,
        @Size(min = 8, max = 100) String password
) {
}
