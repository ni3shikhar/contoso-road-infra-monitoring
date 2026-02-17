// ============================================
// RBAC Types - Matching Backend Exactly
// ============================================

export type Role = 'ADMIN' | 'ENGINEER' | 'OPERATOR' | 'VIEWER';

export type Permission =
  // Sensor permissions
  | 'SENSOR_READ'
  | 'SENSOR_WRITE'
  | 'SENSOR_DELETE'
  | 'SENSOR_CONFIGURE'
  // Asset permissions
  | 'ASSET_READ'
  | 'ASSET_WRITE'
  | 'ASSET_DELETE'
  | 'ASSET_PROGRESS_UPDATE'
  // Monitoring permissions
  | 'MONITORING_READ'
  | 'MONITORING_CONFIGURE_THRESHOLDS'
  // Alert permissions
  | 'ALERT_READ'
  | 'ALERT_ACKNOWLEDGE'
  | 'ALERT_ASSIGN'
  | 'ALERT_RESOLVE'
  | 'ALERT_RULE_MANAGE'
  // Analytics permissions
  | 'ANALYTICS_READ'
  | 'ANALYTICS_EXPORT'
  | 'ANALYTICS_REFRESH'
  // Inspection permissions
  | 'INSPECTION_READ'
  | 'INSPECTION_WRITE'
  // User management permissions
  | 'USER_READ'
  | 'USER_MANAGE'
  // System administration
  | 'SYSTEM_ADMIN';

// Role to permissions mapping - mirrors backend RolePermissionMapping
export const ROLE_PERMISSIONS: Record<Role, Permission[]> = {
  ADMIN: [
    'SENSOR_READ', 'SENSOR_WRITE', 'SENSOR_DELETE', 'SENSOR_CONFIGURE',
    'ASSET_READ', 'ASSET_WRITE', 'ASSET_DELETE', 'ASSET_PROGRESS_UPDATE',
    'MONITORING_READ', 'MONITORING_CONFIGURE_THRESHOLDS',
    'ALERT_READ', 'ALERT_ACKNOWLEDGE', 'ALERT_ASSIGN', 'ALERT_RESOLVE', 'ALERT_RULE_MANAGE',
    'ANALYTICS_READ', 'ANALYTICS_EXPORT', 'ANALYTICS_REFRESH',
    'INSPECTION_READ', 'INSPECTION_WRITE',
    'USER_READ', 'USER_MANAGE',
    'SYSTEM_ADMIN'
  ],
  ENGINEER: [
    'SENSOR_READ', 'SENSOR_WRITE', 'SENSOR_CONFIGURE',
    'ASSET_READ', 'ASSET_WRITE', 'ASSET_PROGRESS_UPDATE',
    'MONITORING_READ', 'MONITORING_CONFIGURE_THRESHOLDS',
    'ALERT_READ', 'ALERT_ACKNOWLEDGE', 'ALERT_ASSIGN', 'ALERT_RESOLVE', 'ALERT_RULE_MANAGE',
    'ANALYTICS_READ', 'ANALYTICS_EXPORT', 'ANALYTICS_REFRESH',
    'INSPECTION_READ', 'INSPECTION_WRITE',
    'USER_READ'
  ],
  OPERATOR: [
    'SENSOR_READ',
    'ASSET_READ', 'ASSET_PROGRESS_UPDATE',
    'MONITORING_READ',
    'ALERT_READ', 'ALERT_ACKNOWLEDGE', 'ALERT_ASSIGN', 'ALERT_RESOLVE',
    'ANALYTICS_READ',
    'INSPECTION_READ', 'INSPECTION_WRITE'
  ],
  VIEWER: [
    'SENSOR_READ',
    'ASSET_READ',
    'MONITORING_READ',
    'ALERT_READ',
    'ANALYTICS_READ',
    'INSPECTION_READ'
  ]
};

export interface User {
  id: string;
  username: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  role: Role;  // Backend returns single role, not array
  persona?: string;
  department?: string;
  permissions?: Permission[];
  enabled?: boolean;
  accountLocked?: boolean;
  mustChangePassword?: boolean;
  lastLogin?: string;
  lastLoginAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
  requiresPasswordChange?: boolean;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface RefreshResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  role: Role;
  persona?: string;
  department?: string;
}

export interface UpdateUserRequest {
  email?: string;
  firstName?: string;
  lastName?: string;
  persona?: string;
  department?: string;
  role?: Role;
}

export interface ChangeRoleRequest {
  newRole: Role;
}

export interface ResetPasswordRequest {
  newPassword: string;
}

// Audit log types
export interface AuditLog {
  id: string;
  userId: string;
  username: string;
  action: string;
  resourceType: string;
  resourceId?: string;
  entityType?: string;
  entityId?: string;
  description?: string;
  ipAddress?: string;
  userAgent?: string;
  details?: Record<string, unknown>;
  timestamp: string;
}

export interface AuditLogFilter {
  userId?: string;
  action?: string;
  resourceType?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}
