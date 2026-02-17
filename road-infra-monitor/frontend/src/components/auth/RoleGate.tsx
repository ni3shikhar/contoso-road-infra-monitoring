import { useAuthStore } from '@/store/authStore';
import type { Role } from '@/types';

interface RoleGateProps {
  roles: Role[];
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

/**
 * Conditionally renders children based on user role.
 * Renders if user has ANY of the specified roles.
 * If user lacks all roles, children are not rendered (hidden, not disabled).
 */
export function RoleGate({ roles, children, fallback = null }: RoleGateProps) {
  const hasAnyRole = useAuthStore((state) => state.hasAnyRole);
  
  if (!hasAnyRole(roles)) {
    return <>{fallback}</>;
  }
  
  return <>{children}</>;
}

interface AdminOnlyProps {
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

/**
 * Shorthand for admin-only content
 */
export function AdminOnly({ children, fallback = null }: AdminOnlyProps) {
  const isAdmin = useAuthStore((state) => state.isAdmin);
  
  if (!isAdmin()) {
    return <>{fallback}</>;
  }
  
  return <>{children}</>;
}

interface NotViewerProps {
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

/**
 * Renders for any role except VIEWER
 */
export function NotViewer({ children, fallback = null }: NotViewerProps) {
  const isViewer = useAuthStore((state) => state.isViewer);
  
  if (isViewer()) {
    return <>{fallback}</>;
  }
  
  return <>{children}</>;
}

interface EngineerOrAboveProps {
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

/**
 * Renders for ADMIN and ENGINEER only
 */
export function EngineerOrAbove({ children, fallback = null }: EngineerOrAboveProps) {
  const hasAnyRole = useAuthStore((state) => state.hasAnyRole);
  
  if (!hasAnyRole(['ADMIN', 'ENGINEER'])) {
    return <>{fallback}</>;
  }
  
  return <>{children}</>;
}
