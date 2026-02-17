import { useAuthStore } from '@/store/authStore';
import type { Permission } from '@/types';

interface PermissionGateProps {
  permission: Permission;
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

/**
 * Conditionally renders children based on user permission.
 * If user lacks the permission, children are not rendered (hidden, not disabled).
 */
export function PermissionGate({ permission, children, fallback = null }: PermissionGateProps) {
  const hasPermission = useAuthStore((state) => state.hasPermission);
  
  if (!hasPermission(permission)) {
    return <>{fallback}</>;
  }
  
  return <>{children}</>;
}

interface MultiPermissionGateProps {
  permissions: Permission[];
  requireAll?: boolean;
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

/**
 * Conditionally renders children based on multiple permissions.
 * By default (requireAll=false), renders if user has ANY of the permissions.
 * Set requireAll=true to require ALL permissions.
 */
export function MultiPermissionGate({ 
  permissions, 
  requireAll = false, 
  children, 
  fallback = null 
}: MultiPermissionGateProps) {
  const hasAnyPermission = useAuthStore((state) => state.hasAnyPermission);
  const hasAllPermissions = useAuthStore((state) => state.hasAllPermissions);
  
  const hasAccess = requireAll 
    ? hasAllPermissions(permissions) 
    : hasAnyPermission(permissions);
  
  if (!hasAccess) {
    return <>{fallback}</>;
  }
  
  return <>{children}</>;
}
