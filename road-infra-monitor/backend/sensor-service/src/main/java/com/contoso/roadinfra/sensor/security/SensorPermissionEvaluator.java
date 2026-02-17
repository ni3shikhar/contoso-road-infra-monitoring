package com.contoso.roadinfra.sensor.security;

import com.contoso.roadinfra.common.constants.Permission;
import com.contoso.roadinfra.common.constants.Role;
import com.contoso.roadinfra.common.security.RolePermissionMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom permission evaluator for sensor-service that reads user roles from JWT claims
 * and checks permissions using the RolePermissionMapping.
 */
@Component("permissionEvaluator")
@Slf4j
public class SensorPermissionEvaluator {

    /**
     * Check if the authenticated user has the specified permission.
     *
     * @param authentication Spring Security authentication object
     * @param permission     The permission to check (as string)
     * @return true if the user has the permission, false otherwise
     */
    public boolean hasPermission(Authentication authentication, String permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("User not authenticated, denying permission: {}", permission);
            return false;
        }

        try {
            Permission requiredPermission = Permission.valueOf(permission);
            Set<Role> userRoles = extractRolesFromAuthentication(authentication);

            if (userRoles.isEmpty()) {
                log.debug("No roles found for user, denying permission: {}", permission);
                return false;
            }

            boolean hasPermission = userRoles.stream()
                    .anyMatch(role -> RolePermissionMapping.hasPermission(role, requiredPermission));

            log.debug("User {} permission check for {}: {} (roles: {})",
                    getUsernameFromAuthentication(authentication),
                    permission,
                    hasPermission,
                    userRoles);

            return hasPermission;
        } catch (IllegalArgumentException e) {
            log.error("Invalid permission name: {}", permission);
            return false;
        }
    }

    /**
     * Check if the user has any of the specified permissions.
     */
    public boolean hasAnyPermission(Authentication authentication, String... permissions) {
        for (String permission : permissions) {
            if (hasPermission(authentication, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the user has all of the specified permissions.
     */
    public boolean hasAllPermissions(Authentication authentication, String... permissions) {
        for (String permission : permissions) {
            if (!hasPermission(authentication, permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the user has a specific role.
     */
    public boolean hasRole(Authentication authentication, String roleName) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        try {
            Role requiredRole = Role.valueOf(roleName);
            Set<Role> userRoles = extractRolesFromAuthentication(authentication);
            return userRoles.contains(requiredRole);
        } catch (IllegalArgumentException e) {
            log.error("Invalid role name: {}", roleName);
            return false;
        }
    }

    /**
     * Extract roles from the authentication object (JWT).
     * Supports both "role" (single role string) and "roles" (comma-separated roles) claims.
     */
    private Set<Role> extractRolesFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            // Try "role" claim first (auth-service uses singular)
            String roleString = jwt.getClaimAsString("role");
            if (roleString != null && !roleString.isBlank()) {
                Role role = parseRole(roleString);
                if (role != null) {
                    return Set.of(role);
                }
            }
            
            // Fallback to "roles" claim (comma-separated)
            String rolesString = jwt.getClaimAsString("roles");
            if (rolesString != null && !rolesString.isBlank()) {
                return Arrays.stream(rolesString.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(this::parseRole)
                        .filter(role -> role != null)
                        .collect(Collectors.toSet());
            }
        }

        return Collections.emptySet();
    }

    /**
     * Get username from authentication.
     */
    public String getUsernameFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            return "anonymous";
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String username = jwt.getClaimAsString("username");
            return username != null ? username : jwt.getSubject();
        }

        return authentication.getName();
    }

    /**
     * Get user ID from authentication.
     */
    public String getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }

        return null;
    }

    private Role parseRole(String roleName) {
        try {
            return Role.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown role in JWT: {}", roleName);
            return null;
        }
    }
}
