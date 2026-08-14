package com.cafe.ps.service;

import com.cafe.ps.dto.CreateUserRequest;
import com.cafe.ps.dto.UpdateUserRequest;
import com.cafe.ps.dto.UserResponse;
import com.cafe.ps.entity.AppUser;
import com.cafe.ps.entity.Role;
import com.cafe.ps.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAllByOrderByUsernameAsc()
                .stream()
                .map(UserService::toResponse)
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
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request, String actorUsername) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean removingAdminAccess = user.getRole() == Role.ADMIN
                && Boolean.TRUE.equals(user.getActive())
                && (request.role() != Role.ADMIN || !request.active());
        if (removingAdminAccess && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new IllegalStateException("The last active administrator cannot be disabled or demoted");
        }
        if (user.getUsername().equalsIgnoreCase(actorUsername) && !request.active()) {
            throw new IllegalStateException("You cannot disable your own account");
        }

        user.setDisplayName(request.displayName().trim());
        user.setRole(request.role());
        user.setActive(request.active());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void recordLogin(String username) {
        AppUser user = requireByUsername(username);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public static UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getActive(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
