package com.cafe.ps.service;

import com.cafe.ps.dto.PermissionResponse;
import com.cafe.ps.dto.RolePermissionsRequest;
import com.cafe.ps.entity.Permission;
import com.cafe.ps.entity.Role;
import com.cafe.ps.entity.RolePermission;
import com.cafe.ps.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final RuleService ruleService;

    @Transactional(readOnly = true)
    public List<PermissionResponse> catalog() {
        Map<Permission, Set<Role>> rolesByPermission = new EnumMap<>(Permission.class);
        rolePermissionRepository.findAll().forEach(rolePermission ->
                rolesByPermission
                        .computeIfAbsent(rolePermission.getPermission(), ignored -> EnumSet.noneOf(Role.class))
                        .add(rolePermission.getRole())
        );

        return Arrays.stream(Permission.values())
                .map(permission -> new PermissionResponse(
                        permission.name(),
                        permission.label(),
                        permission.description(),
                        permission.category(),
                        rolesByPermission.getOrDefault(permission, EnumSet.noneOf(Role.class))
                ))
                .toList();
    }

    @Transactional
    public List<PermissionResponse> updateRole(Role role, RolePermissionsRequest request) {
        Set<Permission> requested = parsePermissions(request == null ? null : request.permissions());
        if (role != Role.ADMIN && requested.contains(Permission.PERMISSIONS_MANAGE)) {
            throw new IllegalArgumentException("Only the ADMIN role can manage permissions");
        }
        if (role == Role.ADMIN) {
            requested.add(Permission.PERMISSIONS_MANAGE);
        }

        rolePermissionRepository.deleteByRole(role);
        rolePermissionRepository.flush();
        rolePermissionRepository.saveAll(requested.stream()
                .map(permission -> RolePermission.builder()
                        .role(role)
                        .permission(permission)
                        .build())
                .toList());
        ruleService.syncSystemRulePermissions(role, requested);
        return catalog();
    }

    @Transactional(readOnly = true)
    public Set<String> codesForRole(Role role) {
        return rolePermissionRepository.findByRoleOrderByPermissionAsc(role)
                .stream()
                .map(rolePermission -> rolePermission.getPermission().name())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
    }

    @Transactional
    public void seedDefaults() {
        Set<String> existingAssignments = new HashSet<>(rolePermissionRepository.findAll().stream()
                .map(rolePermission -> assignmentKey(rolePermission.getRole(), rolePermission.getPermission()))
                .toList());

        List<RolePermission> missingDefaults = Arrays.stream(Permission.values())
                .flatMap(permission -> permission.defaultRoles().stream()
                        .filter(role -> existingAssignments.add(assignmentKey(role, permission)))
                        .map(role -> RolePermission.builder()
                                .role(role)
                                .permission(permission)
                                .build()))
                .toList();

        if (!missingDefaults.isEmpty()) {
            rolePermissionRepository.saveAll(missingDefaults);
        }
    }

    private static String assignmentKey(Role role, Permission permission) {
        return role.name() + ":" + permission.name();
    }

    private static Set<Permission> parsePermissions(Set<String> codes) {
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        if (codes == null) return permissions;

        for (String code : codes) {
            if (code == null || code.isBlank()) continue;
            try {
                permissions.add(Permission.valueOf(code.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unknown permission: " + code);
            }
        }
        return permissions;
    }
}
