package com.contoso.roadinfra.common.security;

import com.contoso.roadinfra.common.constants.Permission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Permission evaluator component for use with @PreAuthorize annotations.
 * 
 * Usage: @PreAuthorize("@perm.has(#root, 'SENSOR_WRITE')")
 *        @PreAuthorize("@perm.hasAny(#root, 'SENSOR_WRITE', 'SENSOR_CONFIGURE')")
 */
@Component("perm")
@Slf4j
public class PermissionEvaluator {

    /**
     * Check if the current user has a specific permission.
     * 
     * @param root the method security expression root (not used, but required for SpEL)
     * @param permission the permission string to check (must match Permission enum name)
     * @return true if user has the permission
     */
    public boolean has(Object root, String permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.debug("Permission check failed: not authenticated");
            return false;
        }

        Set<String> userPermissions = extractPermissions(auth.getAuthorities());
        boolean hasPermission = userPermissions.contains(permission);
        
        log.debug("Permission check for '{}': {} (user has: {})", 
            permission, hasPermission, userPermissions);
        return hasPermission;
    }

    /**
     * Check if the current user has any of the specified permissions.
     */
    public boolean hasAny(Object root, String... permissions) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Set<String> userPermissions = extractPermissions(auth.getAuthorities());
        for (String permission : permissions) {
            if (userPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the current user has all of the specified permissions.
     */
    public boolean hasAll(Object root, String... permissions) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Set<String> userPermissions = extractPermissions(auth.getAuthorities());
        for (String permission : permissions) {
            if (!userPermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the current user has a specific role.
     */
    public boolean hasRole(Object root, String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role) || a.equals(role));
    }

    /**
     * Check if the current user has any of the specified roles.
     */
    public boolean hasAnyRole(Object root, String... roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        for (String role : roles) {
            if (authorities.contains("ROLE_" + role) || authorities.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract permission strings from granted authorities.
     * Permissions are stored as PERM_xxx authorities.
     */
    private Set<String> extractPermissions(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("PERM_"))
                .map(a -> a.substring(5)) // Remove PERM_ prefix
                .collect(Collectors.toSet());
    }

    /**
     * Static utility method to check permission without Spring context.
     */
    public static boolean checkPermission(Authentication auth, Permission permission) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("PERM_" + permission.name()));
    }
}
