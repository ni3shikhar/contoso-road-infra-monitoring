package com.contoso.roadinfra.auth.controller;

import com.contoso.roadinfra.auth.dto.*;
import com.contoso.roadinfra.auth.entity.User;
import com.contoso.roadinfra.auth.service.UserManagementService;
import com.contoso.roadinfra.common.constants.Role;
import com.contoso.roadinfra.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User administration endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping
    @PreAuthorize("@perm.has(#root, 'USER_MANAGE')")
    @Operation(summary = "Create a new user with role and persona (ADMIN only)")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal User currentUser) {
        UserDto user = userManagementService.createUser(request, currentUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User created successfully"));
    }

    @GetMapping
    @PreAuthorize("@perm.has(#root, 'USER_READ')")
    @Operation(summary = "List all users with filtering (ADMIN, OPERATOR)")
    public ResponseEntity<ApiResponse<Page<UserDto>>> listUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean enabled,
            Pageable pageable) {
        Page<UserDto> users = userManagementService.listUsers(role, department, enabled, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has(#root, 'USER_READ')")
    @Operation(summary = "Get user details by ID (ADMIN, OPERATOR)")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable UUID id) {
        UserDto user = userManagementService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has(#root, 'USER_MANAGE')")
    @Operation(summary = "Update user profile (ADMIN only)")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal User currentUser) {
        UserDto user = userManagementService.updateUser(id, request, currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user, "User updated successfully"));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("@perm.has(#root, 'USER_MANAGE')")
    @Operation(summary = "Change user role (ADMIN only)")
    public ResponseEntity<ApiResponse<UserDto>> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal User currentUser) {
        UserDto user = userManagementService.changeUserRole(id, request, currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user, "Role changed successfully"));
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("@perm.has(#root, 'USER_MANAGE')")
    @Operation(summary = "Enable or disable user (ADMIN only)")
    public ResponseEntity<ApiResponse<UserDto>> setEnabled(
            @PathVariable UUID id,
            @RequestParam boolean enabled,
            @AuthenticationPrincipal User currentUser) {
        UserDto user = userManagementService.setUserEnabled(id, enabled, currentUser.getUsername());
        String message = enabled ? "User enabled successfully" : "User disabled successfully";
        return ResponseEntity.ok(ApiResponse.success(user, message));
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("@perm.has(#root, 'USER_MANAGE')")
    @Operation(summary = "Unlock a locked account (ADMIN only)")
    public ResponseEntity<ApiResponse<UserDto>> unlockUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        UserDto user = userManagementService.unlockUser(id, currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user, "User account unlocked"));
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("@perm.has(#root, 'USER_MANAGE')")
    @Operation(summary = "Force password reset on next login (ADMIN only)")
    public ResponseEntity<ApiResponse<UserDto>> forcePasswordReset(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        UserDto user = userManagementService.forcePasswordReset(id, currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user, "Password reset required on next login"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has(#root, 'USER_MANAGE')")
    @Operation(summary = "Soft delete a user (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        userManagementService.deleteUser(id, currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    @GetMapping("/stats")
    @PreAuthorize("@perm.has(#root, 'USER_READ')")
    @Operation(summary = "Get user count statistics by role")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUserStats() {
        Map<String, Long> stats = userManagementService.getUserCountsByRole();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
