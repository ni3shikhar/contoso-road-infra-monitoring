package com.contoso.roadinfra.common.security;

import com.contoso.roadinfra.common.constants.Permission;
import com.contoso.roadinfra.common.constants.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Optional;

/**
 * Utility class for security-related operations.
 * Provides methods to check the current user's role and permissions.
 */
public final class SecurityUtils {

    private static final String ROLE_PREFIX = "ROLE_";

    private SecurityUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Get the current authenticated user's authentication object.
     *
     * @return Optional containing the authentication, or empty if not authenticated
     */
    public static Optional<Authentication> getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return Optional.of(authentication);
        }
        return Optional.empty();
    }

    /**
     * Get the current authenticated user's username.
     *
     * @return Optional containing the username, or empty if not authenticated
     */
    public static Optional<String> getCurrentUsername() {
        return getCurrentAuthentication().map(Authentication::getName);
    }

    /**
     * Get the current authenticated user's role.
     *
     * @return Optional containing the role, or empty if not authenticated or no role found
     */
    public static Optional<Role> getCurrentUserRole() {
        return getCurrentAuthentication()
            .map(Authentication::getAuthorities)
            .flatMap(SecurityUtils::extractRoleFromAuthorities);
    }

    /**
     * Check if the current user has the specified permission.
     *
     * @param permission the permission to check
     * @return true if the user has the permission, false otherwise
     */
    public static boolean hasPermission(Permission permission) {
        return getCurrentUserRole()
            .map(role -> RolePermissionMapping.hasPermission(role, permission))
            .orElse(false);
    }

    /**
     * Check if the current user has any of the specified permissions.
     *
     * @param permissions the permissions to check
     * @return true if the user has at least one of the permissions
     */
    public static boolean hasAnyPermission(Permission... permissions) {
        return getCurrentUserRole()
            .map(role -> RolePermissionMapping.hasAnyPermission(role, permissions))
            .orElse(false);
    }

    /**
     * Check if the current user has all of the specified permissions.
     *
     * @param permissions the permissions to check
     * @return true if the user has all of the permissions
     */
    public static boolean hasAllPermissions(Permission... permissions) {
        return getCurrentUserRole()
            .map(role -> RolePermissionMapping.hasAllPermissions(role, permissions))
            .orElse(false);
    }

    /**
     * Check if the current user has the specified role.
     *
     * @param role the role to check
     * @return true if the user has the role
     */
    public static boolean hasRole(Role role) {
        return getCurrentUserRole()
            .map(currentRole -> currentRole == role)
            .orElse(false);
    }

    /**
     * Check if the current user is an administrator.
     *
     * @return true if the user has ADMIN role
     */
    public static boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    /**
     * Check if the current user is authenticated.
     *
     * @return true if there is an authenticated user
     */
    public static boolean isAuthenticated() {
        return getCurrentAuthentication().isPresent();
    }

    /**
     * Extract role from a collection of granted authorities.
     *
     * @param authorities the collection of authorities
     * @return Optional containing the role if found
     */
    private static Optional<Role> extractRoleFromAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .filter(auth -> auth.startsWith(ROLE_PREFIX))
            .map(auth -> auth.substring(ROLE_PREFIX.length()))
            .map(roleName -> {
                try {
                    return Role.valueOf(roleName);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            })
            .filter(role -> role != null)
            .findFirst();
    }

    /**
     * Require that the current user has the specified permission.
     * Throws AccessDeniedException if the permission is not present.
     *
     * @param permission the required permission
     * @throws org.springframework.security.access.AccessDeniedException if permission is denied
     */
    public static void requirePermission(Permission permission) {
        if (!hasPermission(permission)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Access denied: required permission " + permission.name()
            );
        }
    }

    /**
     * Require that the current user has any of the specified permissions.
     * Throws AccessDeniedException if none of the permissions are present.
     *
     * @param permissions the permissions (at least one required)
     * @throws org.springframework.security.access.AccessDeniedException if permission is denied
     */
    public static void requireAnyPermission(Permission... permissions) {
        if (!hasAnyPermission(permissions)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Access denied: required at least one of the specified permissions"
            );
        }
    }
}
