package com.cafe.ps.service;

import com.cafe.ps.dto.CreateRuleRequest;
import com.cafe.ps.dto.RuleResponse;
import com.cafe.ps.dto.UpdateRuleRequest;
import com.cafe.ps.entity.AccessRule;
import com.cafe.ps.entity.AppUser;
import com.cafe.ps.entity.Permission;
import com.cafe.ps.entity.Role;
import com.cafe.ps.entity.RolePermission;
import com.cafe.ps.entity.RulePermission;
import com.cafe.ps.repository.AccessRuleRepository;
import com.cafe.ps.repository.AppUserRepository;
import com.cafe.ps.repository.RolePermissionRepository;
import com.cafe.ps.repository.RulePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class RuleService {

    private static final Map<Role, String> SYSTEM_RULE_DESCRIPTIONS = Map.of(
            Role.ADMIN, "Full system administration and access control.",
            Role.MANAGER, "Pricing, products, inventory, reports, and daily operations.",
            Role.CASHIER, "Gaming operations, POS, checkout, and cashier shifts."
    );

    private final AccessRuleRepository accessRuleRepository;
    private final RulePermissionRepository rulePermissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public List<RuleResponse> getAll() {
        return accessRuleRepository.findAll()
                .stream()
                .sorted(Comparator
                        .comparing(AccessRule::getSystemRule, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AccessRule::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RuleResponse create(CreateRuleRequest request) {
        String name = normalizeName(request.name());
        assertNameAvailable(name, null);
        AccessRule rule = AccessRule.builder()
                .name(name)
                .description(normalizeDescription(request.description()))
                .systemRule(false)
                .createdAt(LocalDateTime.now())
                .build();
        rule = accessRuleRepository.save(rule);
        replacePermissions(rule, parsePermissions(request.permissions()));
        return toResponse(rule);
    }

    @Transactional
    public RuleResponse update(Long id, UpdateRuleRequest request) {
        AccessRule rule = requireById(id);
        String name = normalizeName(request.name());
        if (Boolean.TRUE.equals(rule.getSystemRule()) && !rule.getName().equals(name)) {
            throw new IllegalStateException("Built-in rule names cannot be changed");
        }
        assertNameAvailable(name, id);

        rule.setName(name);
        rule.setDescription(normalizeDescription(request.description()));
        Set<Permission> permissions = parsePermissions(request.permissions());
        validatePermissionAssignment(rule, permissions);
        if (isAdminSystemRule(rule)) permissions.add(Permission.PERMISSIONS_MANAGE);
        accessRuleRepository.save(rule);
        replacePermissions(rule, permissions);

        if (rule.getSystemRole() != null) {
            syncRolePermissions(rule.getSystemRole(), permissions);
        }
        return toResponse(rule);
    }

    @Transactional
    public void delete(Long id) {
        AccessRule rule = requireById(id);
        if (Boolean.TRUE.equals(rule.getSystemRule())) {
            throw new IllegalStateException("Built-in rules cannot be deleted");
        }
        if (appUserRepository.countByRuleId(id) > 0) {
            throw new IllegalStateException("Reassign users before deleting this rule");
        }
        rulePermissionRepository.deleteByRule(rule);
        accessRuleRepository.delete(rule);
    }

    @Transactional(readOnly = true)
    public AccessRule resolveForUser(Long ruleId, Role role) {
        AccessRule rule = ruleId == null
                ? findSystemRule(role)
                : requireById(ruleId);
        validateUserAssignment(rule, role);
        return rule;
    }

    @Transactional(readOnly = true)
    public Set<String> codesForRule(AccessRule rule) {
        return rulePermissionRepository.findByRuleOrderByPermissionAsc(rule)
                .stream()
                .map(rulePermission -> rulePermission.getPermission().name())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
    }

    @Transactional
    public void syncSystemRulePermissions(Role role, Set<Permission> permissions) {
        AccessRule rule = findSystemRule(role);
        Set<Permission> normalized = EnumSet.noneOf(Permission.class);
        if (permissions != null) normalized.addAll(permissions);
        if (role == Role.ADMIN) normalized.add(Permission.PERMISSIONS_MANAGE);
        validatePermissionAssignment(rule, normalized);
        replacePermissions(rule, normalized);
    }

    /**
     * Creates the three built-in rules and migrates existing users from their
     * legacy role-only permission sets. The migration is intentionally only
     * performed while the new rule-permission table is empty so later startup
     * does not overwrite administrator changes.
     */
    @Transactional
    public void seedDefaults() {
        Map<Role, AccessRule> systemRules = new HashMap<>();
        for (Role role : Role.values()) {
            AccessRule rule = accessRuleRepository.findBySystemRole(role)
                    .orElseGet(() -> accessRuleRepository.save(AccessRule.builder()
                            .name(role.name())
                            .description(SYSTEM_RULE_DESCRIPTIONS.get(role))
                            .systemRule(true)
                            .systemRole(role)
                            .createdAt(LocalDateTime.now())
                            .build()));
            systemRules.put(role, rule);
        }

        boolean migratePermissions = rulePermissionRepository.count() == 0;
        if (migratePermissions) {
            for (Role role : Role.values()) {
                Set<Permission> permissions = rolePermissionRepository
                        .findByRoleOrderByPermissionAsc(role)
                        .stream()
                        .map(com.cafe.ps.entity.RolePermission::getPermission)
                        .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(Permission.class)));
                if (role == Role.ADMIN) permissions.add(Permission.PERMISSIONS_MANAGE);
                replacePermissions(systemRules.get(role), permissions);
            }
        }

        List<AppUser> usersToUpdate = appUserRepository.findAll()
                .stream()
                .filter(user -> user.getRule() == null)
                .peek(user -> user.setRule(systemRules.get(user.getRole() == null ? Role.CASHIER : user.getRole())))
                .toList();
        if (!usersToUpdate.isEmpty()) appUserRepository.saveAll(usersToUpdate);
    }

    private AccessRule requireById(Long id) {
        return accessRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found"));
    }

    private AccessRule findSystemRule(Role role) {
        Role resolvedRole = role == null ? Role.CASHIER : role;
        return accessRuleRepository.findBySystemRole(resolvedRole)
                .orElseThrow(() -> new IllegalStateException("The built-in " + resolvedRole + " rule is not configured"));
    }

    private void assertNameAvailable(String name, Long currentId) {
        accessRuleRepository.findByNameIgnoreCase(name)
                .filter(rule -> currentId == null || !rule.getId().equals(currentId))
                .ifPresent(rule -> {
                    throw new IllegalStateException("A rule with this name already exists");
                });
    }

    private void validateUserAssignment(AccessRule rule, Role role) {
        if (rule.getSystemRole() != null && rule.getSystemRole() != role) {
            throw new IllegalArgumentException("The selected built-in rule must match the user's role");
        }
        if (role == Role.ADMIN && !isAdminSystemRule(rule)) {
            throw new IllegalArgumentException("ADMIN users must use the built-in ADMIN rule");
        }
    }

    private void validatePermissionAssignment(AccessRule rule, Set<Permission> permissions) {
        if (permissions.contains(Permission.PERMISSIONS_MANAGE) && !isAdminSystemRule(rule)) {
            throw new IllegalArgumentException("Only the built-in ADMIN rule can manage permissions");
        }
    }

    private boolean isAdminSystemRule(AccessRule rule) {
        return Boolean.TRUE.equals(rule.getSystemRule()) && rule.getSystemRole() == Role.ADMIN;
    }

    private void replacePermissions(AccessRule rule, Set<Permission> permissions) {
        rulePermissionRepository.deleteByRule(rule);
        if (permissions == null || permissions.isEmpty()) return;
        rulePermissionRepository.saveAll(permissions.stream()
                .map(permission -> RulePermission.builder()
                        .rule(rule)
                        .permission(permission)
                        .build())
                .toList());
    }

    private void syncRolePermissions(Role role, Set<Permission> permissions) {
        rolePermissionRepository.deleteByRole(role);
        rolePermissionRepository.saveAll(permissions.stream()
                .map(permission -> RolePermission.builder()
                        .role(role)
                        .permission(permission)
                        .build())
                .toList());
    }

    private RuleResponse toResponse(AccessRule rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getName(),
                rule.getDescription(),
                rule.getSystemRule(),
                rule.getSystemRole(),
                appUserRepository.countByRuleId(rule.getId()),
                codesForRule(rule)
        );
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

    private static String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private static String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }
}
