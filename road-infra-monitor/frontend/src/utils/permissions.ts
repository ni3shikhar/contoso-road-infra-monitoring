import { Permission, Role } from '../types';

/**
 * Role to permissions mapping matching backend RolePermissionMapping.java
 */
const ROLE_PERMISSIONS: Record<Role, Permission[]> = {
  ADMIN: [
    // All permissions
    'SENSOR_READ', 'SENSOR_WRITE', 'SENSOR_DELETE', 'SENSOR_CONFIGURE',
    'ASSET_READ', 'ASSET_WRITE', 'ASSET_DELETE', 'ASSET_PROGRESS_UPDATE',
    'MONITORING_READ', 'MONITORING_CONFIGURE_THRESHOLDS',
    'ALERT_READ', 'ALERT_ACKNOWLEDGE', 'ALERT_ASSIGN', 'ALERT_RESOLVE', 'ALERT_RULE_MANAGE',
    'ANALYTICS_READ', 'ANALYTICS_EXPORT', 'ANALYTICS_REFRESH',
    'INSPECTION_READ', 'INSPECTION_WRITE',
    'USER_READ', 'USER_MANAGE', 'SYSTEM_ADMIN',
  ],
  ENGINEER: [
    'SENSOR_READ', 'SENSOR_WRITE', 'SENSOR_CONFIGURE',
    'ASSET_READ', 'ASSET_WRITE', 'ASSET_PROGRESS_UPDATE',
    'MONITORING_READ', 'MONITORING_CONFIGURE_THRESHOLDS',
    'ALERT_READ', 'ALERT_ACKNOWLEDGE', 'ALERT_ASSIGN', 'ALERT_RESOLVE', 'ALERT_RULE_MANAGE',
    'ANALYTICS_READ', 'ANALYTICS_EXPORT', 'ANALYTICS_REFRESH',
    'INSPECTION_READ', 'INSPECTION_WRITE',
  ],
  OPERATOR: [
    'SENSOR_READ',
    'ASSET_READ', 'ASSET_PROGRESS_UPDATE',
    'MONITORING_READ',
    'ALERT_READ', 'ALERT_ACKNOWLEDGE', 'ALERT_ASSIGN', 'ALERT_RESOLVE',
    'ANALYTICS_READ',
    'INSPECTION_READ', 'INSPECTION_WRITE',
  ],
  VIEWER: [
    'SENSOR_READ',
    'ASSET_READ',
    'MONITORING_READ',
    'ALERT_READ',
    'ANALYTICS_READ',
    'INSPECTION_READ',
  ],
};

/**
 * Get all permissions for a given role.
 */
export function getPermissionsForRole(role: Role): Permission[] {
  return ROLE_PERMISSIONS[role] || [];
}

/**
 * Check if a role has a specific permission.
 */
export function roleHasPermission(role: Role, permission: Permission): boolean {
  return ROLE_PERMISSIONS[role]?.includes(permission) ?? false;
}

/**
 * Check if a role has any of the specified permissions.
 */
export function roleHasAnyPermission(role: Role, permissions: Permission[]): boolean {
  const rolePermissions = ROLE_PERMISSIONS[role] || [];
  return permissions.some(p => rolePermissions.includes(p));
}

/**
 * Check if a role has all of the specified permissions.
 */
export function roleHasAllPermissions(role: Role, permissions: Permission[]): boolean {
  const rolePermissions = ROLE_PERMISSIONS[role] || [];
  return permissions.every(p => rolePermissions.includes(p));
}

/**
 * Get all roles that have a specific permission.
 */
export function getRolesWithPermission(permission: Permission): Role[] {
  return (Object.keys(ROLE_PERMISSIONS) as Role[]).filter(
    role => ROLE_PERMISSIONS[role].includes(permission)
  );
}

/**
 * Permission groups for UI display.
 */
export const PERMISSION_GROUPS = {
  sensor: ['SENSOR_READ', 'SENSOR_WRITE', 'SENSOR_DELETE', 'SENSOR_CONFIGURE'] as Permission[],
  asset: ['ASSET_READ', 'ASSET_WRITE', 'ASSET_DELETE', 'ASSET_PROGRESS_UPDATE'] as Permission[],
  monitoring: ['MONITORING_READ', 'MONITORING_CONFIGURE_THRESHOLDS'] as Permission[],
  alert: ['ALERT_READ', 'ALERT_ACKNOWLEDGE', 'ALERT_ASSIGN', 'ALERT_RESOLVE', 'ALERT_RULE_MANAGE'] as Permission[],
  analytics: ['ANALYTICS_READ', 'ANALYTICS_EXPORT', 'ANALYTICS_REFRESH'] as Permission[],
  inspection: ['INSPECTION_READ', 'INSPECTION_WRITE'] as Permission[],
  admin: ['USER_READ', 'USER_MANAGE', 'SYSTEM_ADMIN'] as Permission[],
};

/**
 * Human-readable permission labels.
 */
export const PERMISSION_LABELS: Record<Permission, string> = {
  SENSOR_READ: 'Read sensor data',
  SENSOR_WRITE: 'Create/update sensors',
  SENSOR_DELETE: 'Delete sensors',
  SENSOR_CONFIGURE: 'Configure sensor settings',
  ASSET_READ: 'Read asset data',
  ASSET_WRITE: 'Create/update assets',
  ASSET_DELETE: 'Delete assets',
  ASSET_PROGRESS_UPDATE: 'Update asset progress',
  MONITORING_READ: 'Read monitoring data',
  MONITORING_CONFIGURE_THRESHOLDS: 'Configure thresholds',
  ALERT_READ: 'Read alerts',
  ALERT_ACKNOWLEDGE: 'Acknowledge alerts',
  ALERT_ASSIGN: 'Assign alerts',
  ALERT_RESOLVE: 'Resolve alerts',
  ALERT_RULE_MANAGE: 'Manage alert rules',
  ANALYTICS_READ: 'Read analytics',
  ANALYTICS_EXPORT: 'Export data',
  ANALYTICS_REFRESH: 'Refresh analytics',
  INSPECTION_READ: 'Read inspections',
  INSPECTION_WRITE: 'Create/update inspections',
  USER_READ: 'Read user data',
  USER_MANAGE: 'Manage users',
  SYSTEM_ADMIN: 'System administration',
};

/**
 * Human-readable role labels and descriptions.
 */
export const ROLE_INFO: Record<Role, { label: string; description: string }> = {
  ADMIN: {
    label: 'Administrator',
    description: 'Full system access including user management',
  },
  ENGINEER: {
    label: 'Engineer',
    description: 'Technical access for configuration and analysis',
  },
  OPERATOR: {
    label: 'Operator',
    description: 'Operational access for monitoring and incident response',
  },
  VIEWER: {
    label: 'Viewer',
    description: 'Read-only access to all resources',
  },
};
