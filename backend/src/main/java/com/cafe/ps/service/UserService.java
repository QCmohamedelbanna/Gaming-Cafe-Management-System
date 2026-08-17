package com.cafe.ps.service;

import com.cafe.ps.dto.CreateUserRequest;
import com.cafe.ps.dto.UpdateUserRequest;
import com.cafe.ps.dto.UserResponse;
import com.cafe.ps.entity.AppUser;
import com.cafe.ps.entity.AccessRule;
import com.cafe.ps.entity.Role;
import com.cafe.ps.repository.RolePermissionRepository;
import com.cafe.ps.repository.AppUserRepository;
import com.cafe.ps.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RuleService ruleService;
    private final ShiftRepository shiftRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAllByOrderByUsernameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppUser requireByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String username) {
        return toResponse(requireByUsername(username));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalStateException("A user with this username already exists");
        }

        AppUser user = AppUser.builder()
                .username(username)
                .displayName(request.displayName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .rule(ruleService.resolveForUser(request.ruleId(), request.role()))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request, String actorUsername) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        AccessRule requestedRule = ruleService.resolveForUser(request.ruleId(), request.role());
        boolean removingAdminAccess = user.getRole() == Role.ADMIN
                && Boolean.TRUE.equals(user.getActive())
                && (request.role() != Role.ADMIN
                || requestedRule.getSystemRole() != Role.ADMIN
                || !request.active());
        if (removingAdminAccess && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new IllegalStateException("The last active administrator cannot be disabled or demoted");
        }
        if (user.getUsername().equalsIgnoreCase(actorUsername) && !request.active()) {
            throw new IllegalStateException("You cannot disable your own account");
        }

        user.setDisplayName(request.displayName().trim());
        user.setRole(request.role());
        user.setRule(requestedRule);
        user.setActive(request.active());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse setActive(Long id, boolean active, String actorUsername) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!active) {
            assertCanDeactivate(user, actorUsername);
        }

        user.setActive(active);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id, String actorUsername) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getUsername().equalsIgnoreCase(actorUsername)) {
            throw new IllegalStateException("You cannot delete your own account");
        }
        if (user.getRole() == Role.ADMIN
                && Boolean.TRUE.equals(user.getActive())
                && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new IllegalStateException("The last active administrator cannot be deleted");
        }
        if (shiftRepository.countByUserId(id) > 0) {
            throw new IllegalStateException("Users with shift history cannot be deleted; deactivate the account instead");
        }

        userRepository.delete(user);
    }

    @Transactional
    public void recordLogin(String username) {
        AppUser user = requireByUsername(username);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getRule() == null ? null : user.getRule().getId(),
                user.getRule() == null ? user.getRole().name() : user.getRule().getName(),
                user.getActive(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.getRule() == null
                        ? rolePermissionRepository.findByRoleOrderByPermissionAsc(user.getRole())
                        .stream()
                        .map(rolePermission -> rolePermission.getPermission().name())
                        .collect(Collectors.toCollection(TreeSet::new))
                        : ruleService.codesForRule(user.getRule())
        );
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private void assertCanDeactivate(AppUser user, String actorUsername) {
        if (user.getUsername().equalsIgnoreCase(actorUsername)) {
            throw new IllegalStateException("You cannot disable your own account");
        }
        if (user.getRole() == Role.ADMIN
                && Boolean.TRUE.equals(user.getActive())
                && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new IllegalStateException("The last active administrator cannot be disabled or demoted");
        }
    }
}
