package com.contoso.roadinfra.auth.service;

import com.contoso.roadinfra.auth.dto.*;
import com.contoso.roadinfra.auth.entity.AuditLog;
import com.contoso.roadinfra.auth.entity.User;
import com.contoso.roadinfra.auth.repository.RefreshTokenRepository;
import com.contoso.roadinfra.auth.repository.UserRepository;
import com.contoso.roadinfra.common.constants.Role;
import com.contoso.roadinfra.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserManagementService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    /**
     * Create a new user.
     */
    public UserDto createUser(CreateUserRequest request, String createdBy) {
        log.info("Creating user: {} by {}", request.getUsername(), createdBy);

        // Check for existing username or email
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .persona(request.getPersona())
                .department(request.getDepartment())
                .phoneNumber(request.getPhoneNumber())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .mustChangePassword(request.getMustChangePassword() != null ? request.getMustChangePassword() : false)
                .passwordChangedAt(Instant.now())
                .createdBy(createdBy)
                .build();

        user = userRepository.save(user);

        Map<String, Object> details = new HashMap<>();
        details.put("role", request.getRole().name());
        details.put("persona", request.getPersona());
        details.put("department", request.getDepartment());
        auditService.logUserManagementEvent(createdBy, AuditLog.Actions.CREATE_USER, 
                user.getId(), user.getUsername(), details);

        log.info("User {} created successfully with role {}", user.getUsername(), user.getRole());
        return UserDto.fromEntity(user);
    }

    /**
     * Get user by ID.
     */
    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserDto.fromEntity(user);
    }

    /**
     * List all users with filtering.
     */
    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(Role role, String department, Boolean enabled, Pageable pageable) {
        return userRepository.findByFilters(role, department, enabled, pageable)
                .map(UserDto::fromEntity);
    }

    /**
     * Update user profile.
     */
    public UserDto updateUser(UUID userId, UpdateUserRequest request, String updatedBy) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Map<String, Object> changes = new HashMap<>();

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
            changes.put("email", Map.of("from", user.getEmail(), "to", request.getEmail()));
            user.setEmail(request.getEmail());
        }
        if (request.getFirstName() != null) {
            changes.put("firstName", Map.of("from", user.getFirstName(), "to", request.getFirstName()));
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            changes.put("lastName", Map.of("from", user.getLastName(), "to", request.getLastName()));
            user.setLastName(request.getLastName());
        }
        if (request.getPersona() != null) {
            changes.put("persona", Map.of("from", user.getPersona(), "to", request.getPersona()));
            user.setPersona(request.getPersona());
        }
        if (request.getDepartment() != null) {
            changes.put("department", Map.of("from", user.getDepartment(), "to", request.getDepartment()));
            user.setDepartment(request.getDepartment());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        user = userRepository.save(user);

        auditService.logUserManagementEvent(updatedBy, AuditLog.Actions.UPDATE_USER, 
                user.getId(), user.getUsername(), changes);

        log.info("User {} updated by {}", user.getUsername(), updatedBy);
        return UserDto.fromEntity(user);
    }

    /**
     * Change user role.
     */
    public UserDto changeUserRole(UUID userId, ChangeRoleRequest request, String changedBy) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Role oldRole = user.getRole();
        userRepository.updateRole(userId, request.getRole());

        if (request.getPersona() != null) {
            user.setPersona(request.getPersona());
            userRepository.save(user);
        }

        // Revoke all tokens to force re-login with new permissions
        refreshTokenRepository.revokeAllByUser(userId, LocalDateTime.now());

        Map<String, Object> changes = new HashMap<>();
        changes.put("role", Map.of("from", oldRole.name(), "to", request.getRole().name()));
        if (request.getPersona() != null) {
            changes.put("persona", request.getPersona());
        }
        auditService.logUserManagementEvent(changedBy, AuditLog.Actions.CHANGE_ROLE, 
                userId, user.getUsername(), changes);

        log.info("User {} role changed from {} to {} by {}", 
                user.getUsername(), oldRole, request.getRole(), changedBy);

        // Refresh user data
        user = userRepository.findById(userId).orElse(user);
        return UserDto.fromEntity(user);
    }

    /**
     * Enable or disable user.
     */
    public UserDto setUserEnabled(UUID userId, boolean enabled, String changedBy) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        userRepository.setEnabled(userId, enabled);

        if (!enabled) {
            // Revoke all tokens when disabling
            refreshTokenRepository.revokeAllByUser(userId, LocalDateTime.now());
        }

        String action = enabled ? AuditLog.Actions.ENABLE_USER : AuditLog.Actions.DISABLE_USER;
        auditService.logUserManagementEvent(changedBy, action, userId, user.getUsername(), null);

        log.info("User {} {} by {}", user.getUsername(), enabled ? "enabled" : "disabled", changedBy);

        user = userRepository.findById(userId).orElse(user);
        return UserDto.fromEntity(user);
    }

    /**
     * Unlock a locked account.
     */
    public UserDto unlockUser(UUID userId, String unlockedBy) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        userRepository.unlockAccount(userId);

        auditService.logUserManagementEvent(unlockedBy, AuditLog.Actions.UNLOCK_USER, 
                userId, user.getUsername(), null);

        log.info("User {} unlocked by {}", user.getUsername(), unlockedBy);

        user = userRepository.findById(userId).orElse(user);
        return UserDto.fromEntity(user);
    }

    /**
     * Force password reset for a user.
     */
    public UserDto forcePasswordReset(UUID userId, String resetBy) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        userRepository.forcePasswordReset(userId);

        // Revoke all tokens to force re-login
        refreshTokenRepository.revokeAllByUser(userId, LocalDateTime.now());

        auditService.logUserManagementEvent(resetBy, AuditLog.Actions.PASSWORD_RESET, 
                userId, user.getUsername(), null);

        log.info("Password reset forced for user {} by {}", user.getUsername(), resetBy);

        user = userRepository.findById(userId).orElse(user);
        return UserDto.fromEntity(user);
    }

    /**
     * Soft delete a user.
     */
    public void deleteUser(UUID userId, String deletedBy) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        userRepository.softDelete(userId);

        // Revoke all tokens
        refreshTokenRepository.revokeAllByUser(userId, LocalDateTime.now());

        auditService.logUserManagementEvent(deletedBy, AuditLog.Actions.DELETE_USER, 
                userId, user.getUsername(), null);

        log.info("User {} deleted by {}", user.getUsername(), deletedBy);
    }

    /**
     * Get user statistics by role.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getUserCountsByRole() {
        Map<String, Long> counts = new HashMap<>();
        for (Role role : Role.values()) {
            counts.put(role.name(), userRepository.countByRole(role));
        }
        return counts;
    }
}
