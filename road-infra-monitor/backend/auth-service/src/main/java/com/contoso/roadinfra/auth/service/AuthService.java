package com.contoso.roadinfra.auth.service;

import com.contoso.roadinfra.auth.dto.*;
import com.contoso.roadinfra.auth.entity.AuditLog;
import com.contoso.roadinfra.auth.entity.RefreshToken;
import com.contoso.roadinfra.auth.entity.User;
import com.contoso.roadinfra.auth.repository.RefreshTokenRepository;
import com.contoso.roadinfra.auth.repository.UserRepository;
import com.contoso.roadinfra.auth.security.JwtTokenProvider;
import com.contoso.roadinfra.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    @Value("${auth.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${auth.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    /**
     * Authenticate user and return JWT tokens.
     * Account is locked after 5 failed attempts.
     */
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for user: {}", request.getUsername());
        String clientIp = getClientIp(httpRequest);

        User user = userRepository.findByUsernameAndDeletedFalse(request.getUsername())
                .orElseThrow(() -> {
                    auditService.logAuthEvent(request.getUsername(), AuditLog.Actions.LOGIN_FAILED, false, clientIp);
                    return new BadCredentialsException("Invalid username or password");
                });

        // Check if account is locked
        if (!user.isAccountNonLocked()) {
            log.warn("Login attempt for locked account: {}", user.getUsername());
            auditService.logAuthEvent(user.getUsername(), AuditLog.Actions.LOGIN_FAILED, false, clientIp);
            throw new BadCredentialsException("Account is locked. Try again later.");
        }

        // Check if account is enabled
        if (!user.getEnabled()) {
            log.warn("Login attempt for disabled account: {}", user.getUsername());
            auditService.logAuthEvent(user.getUsername(), AuditLog.Actions.LOGIN_FAILED, false, clientIp);
            throw new BadCredentialsException("Account is disabled. Contact administrator.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user, clientIp);
            throw new BadCredentialsException("Invalid username or password");
        }

        // Reset failed attempts on successful login
        userRepository.resetFailedAttempts(user.getId());
        userRepository.updateLastLogin(user.getId(), Instant.now(), clientIp);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user, httpRequest);

        // Log successful login
        auditService.logAuthEvent(user.getUsername(), AuditLog.Actions.LOGIN, true, clientIp);
        log.info("User {} logged in successfully from {}", user.getUsername(), clientIp);

        // Refresh the user to get updated lastLoginAt
        user = userRepository.findById(user.getId()).orElse(user);

        return AuthResponse.of(accessToken, refreshToken.getToken(),
                jwtTokenProvider.getAccessTokenExpiration() / 1000, user);
    }

    /**
     * Refresh access token using refresh token.
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!refreshToken.isActive()) {
            throw new BadCredentialsException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();
        if (!user.getEnabled() || user.getDeleted()) {
            throw new BadCredentialsException("User account is no longer active");
        }

        // Revoke old token
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken newRefreshToken = createRefreshToken(user, null);
        refreshToken.setReplacedBy(newRefreshToken.getToken());

        refreshTokenRepository.save(refreshToken);

        auditService.logAuthEvent(user.getUsername(), AuditLog.Actions.TOKEN_REFRESH, true, null);
        log.debug("Token refreshed for user {}", user.getUsername());

        return AuthResponse.of(newAccessToken, newRefreshToken.getToken(),
                jwtTokenProvider.getAccessTokenExpiration() / 1000, user);
    }

    /**
     * Logout user by invalidating refresh token.
     */
    public void logout(String refreshToken, String username) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                    auditService.logAuthEvent(token.getUser().getUsername(), AuditLog.Actions.LOGOUT, true, null);
                    log.info("User {} logged out", token.getUser().getUsername());
                });
    }

    /**
     * Logout user from all devices.
     */
    public void logoutAll(User user) {
        refreshTokenRepository.revokeAllByUser(user.getId(), LocalDateTime.now());
        auditService.logAuthEvent(user.getUsername(), AuditLog.Actions.LOGOUT, true, null);
        log.info("All sessions revoked for user {}", user.getUsername());
    }

    /**
     * Change password for authenticated user.
     */
    public void changePassword(User user, ChangePasswordRequest request) {
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Verify new password matches confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        // Update password
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        userRepository.updatePassword(user.getId(), newPasswordHash, Instant.now());

        // Revoke all refresh tokens to force re-login
        refreshTokenRepository.revokeAllByUser(user.getId(), LocalDateTime.now());

        auditService.logAuthEvent(user.getUsername(), AuditLog.Actions.PASSWORD_CHANGE, true, null);
        log.info("Password changed for user {}", user.getUsername());
    }

    /**
     * Get current user profile.
     */
    @Transactional(readOnly = true)
    public AuthResponse.UserInfo getCurrentUser(User user) {
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .persona(user.getPersona())
                .department(user.getDepartment())
                .permissions(user.getPermissions().stream()
                        .map(Enum::name)
                        .sorted()
                        .collect(java.util.stream.Collectors.toList()))
                .lastLoginAt(user.getLastLoginAt())
                .mustChangePassword(user.getMustChangePassword())
                .build();
    }

    private RefreshToken createRefreshToken(User user, HttpServletRequest request) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(jwtTokenProvider.generateRefreshToken(user.getId()))
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000))
                .userAgent(request != null ? truncate(request.getHeader("User-Agent"), 255) : null)
                .ipAddress(request != null ? getClientIp(request) : null)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    private void handleFailedLogin(User user, String clientIp) {
        userRepository.incrementFailedAttempts(user.getId());
        int newAttempts = user.getFailedLoginAttempts() + 1;
        
        auditService.logAuthEvent(user.getUsername(), AuditLog.Actions.LOGIN_FAILED, false, clientIp);

        if (newAttempts >= maxFailedAttempts) {
            Instant lockUntil = Instant.now().plus(lockDurationMinutes, ChronoUnit.MINUTES);
            userRepository.lockAccount(user.getId(), lockUntil);
            log.warn("Account {} locked until {} after {} failed attempts", 
                    user.getUsername(), lockUntil, newAttempts);
            
            Map<String, Object> details = new HashMap<>();
            details.put("failedAttempts", newAttempts);
            details.put("lockUntil", lockUntil.toString());
            auditService.logUserManagementEvent("SYSTEM", AuditLog.Actions.LOCK_USER, 
                    user.getId(), user.getUsername(), details);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
