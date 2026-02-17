package com.contoso.roadinfra.common.security;

import com.contoso.roadinfra.common.constants.Permission;
import com.contoso.roadinfra.common.constants.Role;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Static utility class mapping each Role to its set of Permissions.
 * 
 * Role Permission Breakdown:
 * 
 * ADMIN -> ALL permissions (full system access)
 * 
 * ENGINEER -> Technical access for Structural/Civil Engineers, IoT Techs, Data Analysts
 *   - SENSOR: READ, WRITE, CONFIGURE
 *   - ASSET: READ, WRITE, PROGRESS_UPDATE
 *   - MONITORING: READ, CONFIGURE_THRESHOLDS
 *   - ALERT: READ, ACKNOWLEDGE, ASSIGN, RESOLVE, RULE_MANAGE
 *   - ANALYTICS: READ, EXPORT, REFRESH
 *   - INSPECTION: READ, WRITE
 * 
 * OPERATOR -> Operational access for Site Managers, Maintenance Managers, Safety Officers
 *   - SENSOR: READ
 *   - ASSET: READ, PROGRESS_UPDATE
 *   - MONITORING: READ
 *   - ALERT: READ, ACKNOWLEDGE, ASSIGN, RESOLVE
 *   - ANALYTICS: READ
 *   - INSPECTION: READ, WRITE
 * 
 * VIEWER -> Read-only access for Executives, Regulatory Inspectors
 *   - SENSOR: READ
 *   - ASSET: READ
 *   - MONITORING: READ
 *   - ALERT: READ
 *   - ANALYTICS: READ
 *   - INSPECTION: READ
 */
public final class RolePermissionMapping {

    private static final Map<Role, Set<Permission>> ROLE_PERMISSIONS;

    static {
        Map<Role, Set<Permission>> map = new EnumMap<>(Role.class);

        // ADMIN - All permissions
        map.put(Role.ADMIN, EnumSet.allOf(Permission.class));

        // ENGINEER - Technical access
        map.put(Role.ENGINEER, EnumSet.of(
            // Sensor permissions
            Permission.SENSOR_READ,
            Permission.SENSOR_WRITE,
            Permission.SENSOR_CONFIGURE,
            // Asset permissions
            Permission.ASSET_READ,
            Permission.ASSET_WRITE,
            Permission.ASSET_PROGRESS_UPDATE,
            // Monitoring permissions
            Permission.MONITORING_READ,
            Permission.MONITORING_CONFIGURE_THRESHOLDS,
            // Alert permissions
            Permission.ALERT_READ,
            Permission.ALERT_ACKNOWLEDGE,
            Permission.ALERT_ASSIGN,
            Permission.ALERT_RESOLVE,
            Permission.ALERT_RULE_MANAGE,
            // Analytics permissions
            Permission.ANALYTICS_READ,
            Permission.ANALYTICS_EXPORT,
            Permission.ANALYTICS_REFRESH,
            // Inspection permissions
            Permission.INSPECTION_READ,
            Permission.INSPECTION_WRITE
        ));

        // OPERATOR - Operational access
        map.put(Role.OPERATOR, EnumSet.of(
            // Sensor permissions
            Permission.SENSOR_READ,
            // Asset permissions
            Permission.ASSET_READ,
            Permission.ASSET_PROGRESS_UPDATE,
            // Monitoring permissions
            Permission.MONITORING_READ,
            // Alert permissions
            Permission.ALERT_READ,
            Permission.ALERT_ACKNOWLEDGE,
            Permission.ALERT_ASSIGN,
            Permission.ALERT_RESOLVE,
            // Analytics permissions
            Permission.ANALYTICS_READ,
            // Inspection permissions
            Permission.INSPECTION_READ,
            Permission.INSPECTION_WRITE,
            // User permissions (read-only for operators)
            Permission.USER_READ
        ));

        // VIEWER - Read-only access
        map.put(Role.VIEWER, EnumSet.of(
            Permission.SENSOR_READ,
            Permission.ASSET_READ,
            Permission.MONITORING_READ,
            Permission.ALERT_READ,
            Permission.ANALYTICS_READ,
            Permission.INSPECTION_READ
        ));

        ROLE_PERMISSIONS = Collections.unmodifiableMap(map);
    }

    private RolePermissionMapping() {
        // Utility class - prevent instantiation
    }

    /**
     * Get all permissions for a given role.
     *
     * @param role the role to get permissions for
     * @return unmodifiable set of permissions for the role
     */
    public static Set<Permission> getPermissions(Role role) {
        return ROLE_PERMISSIONS.getOrDefault(role, Collections.emptySet());
    }

    /**
     * Check if a role has a specific permission.
     *
     * @param role the role to check
     * @param permission the permission to check for
     * @return true if the role has the permission
     */
    public static boolean hasPermission(Role role, Permission permission) {
        return getPermissions(role).contains(permission);
    }

    /**
     * Check if a role has any of the specified permissions.
     *
     * @param role the role to check
     * @param permissions the permissions to check for
     * @return true if the role has at least one of the permissions
     */
    public static boolean hasAnyPermission(Role role, Permission... permissions) {
        Set<Permission> rolePermissions = getPermissions(role);
        for (Permission permission : permissions) {
            if (rolePermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a role has all of the specified permissions.
     *
     * @param role the role to check
     * @param permissions the permissions to check for
     * @return true if the role has all of the permissions
     */
    public static boolean hasAllPermissions(Role role, Permission... permissions) {
        Set<Permission> rolePermissions = getPermissions(role);
        for (Permission permission : permissions) {
            if (!rolePermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get all roles that have a specific permission.
     *
     * @param permission the permission to search for
     * @return set of roles that have the permission
     */
    public static Set<Role> getRolesWithPermission(Permission permission) {
        EnumSet<Role> roles = EnumSet.noneOf(Role.class);
        for (Map.Entry<Role, Set<Permission>> entry : ROLE_PERMISSIONS.entrySet()) {
            if (entry.getValue().contains(permission)) {
                roles.add(entry.getKey());
            }
        }
        return roles;
    }
}
