package com.cafe.ps.dto;

import java.util.Set;

public record RolePermissionsRequest(Set<String> permissions) {
}
