package com.contoso.roadinfra.auth.controller;

import com.contoso.roadinfra.auth.dto.*;
import com.contoso.roadinfra.auth.entity.User;
import com.contoso.roadinfra.auth.service.AuthService;
import com.contoso.roadinfra.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and session management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get JWT tokens",
               description = "Returns access token (15 min) and refresh token (7 days). Account locks after 5 failed attempts.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate refresh token",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody RefreshTokenRequest request,
            @AuthenticationPrincipal User user) {
        authService.logout(request.getRefreshToken(), user != null ? user.getUsername() : null);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> logoutAll(@AuthenticationPrincipal User user) {
        authService.logoutAll(user);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out from all devices"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile with role and permissions",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser(
            @AuthenticationPrincipal User user) {
        AuthResponse.UserInfo userInfo = authService.getCurrentUser(user);
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change own password",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully. Please login again."));
    }
}
