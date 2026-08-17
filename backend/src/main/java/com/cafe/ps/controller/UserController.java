package com.cafe.ps.controller;

import com.cafe.ps.dto.CreateUserRequest;
import com.cafe.ps.dto.UpdateUserRequest;
import com.cafe.ps.dto.UpdateUserStatusRequest;
import com.cafe.ps.dto.UserResponse;
import com.cafe.ps.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
@PreAuthorize("hasAuthority('PERMISSION_USERS_MANAGE')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> all() {
        return userService.getAll();
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication
    ) {
        return userService.update(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/active")
    public UserResponse setActive(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication
    ) {
        return userService.setActive(id, request.active(), authentication.getName());
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        userService.delete(id, authentication.getName());
    }
}
