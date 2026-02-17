import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore, useAuthHydration } from '@/store/authStore';
import { useToast } from '@/components/ui/use-toast';
import { useEffect, useRef } from 'react';
import type { Role, Permission } from '@/types';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: Role;
  requiredRoles?: Role[];
  requiredPermission?: Permission;
  requiredPermissions?: Permission[];
  requireAllPermissions?: boolean;
}

/**
 * Protected route wrapper that checks authentication and authorization.
 * - Unauthenticated users are redirected to /login
 * - Unauthorized users are redirected to dashboard with 'Access Denied' toast
 */
export function ProtectedRoute({
  children,
  requiredRole,
  requiredRoles,
  requiredPermission,
  requiredPermissions,
  requireAllPermissions = false,
}: ProtectedRouteProps) {
  const location = useLocation();
  const { toast } = useToast();
  const toastShownRef = useRef(false);
  
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const hasHydrated = useAuthHydration();
  const mustChangePassword = useAuthStore((state) => state.mustChangePassword);
  const hasRole = useAuthStore((state) => state.hasRole);
  const hasAnyRole = useAuthStore((state) => state.hasAnyRole);
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const hasAnyPermission = useAuthStore((state) => state.hasAnyPermission);
  const hasAllPermissions = useAuthStore((state) => state.hasAllPermissions);

  // Wait for hydration before checking auth state
  if (!hasHydrated) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  // Check if user is authenticated
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Check if user must change password (except for the change password route)
  if (mustChangePassword && location.pathname !== '/change-password') {
    return <Navigate to="/change-password" replace />;
  }

  // Check role requirements
  let hasRequiredRole = true;
  if (requiredRole) {
    hasRequiredRole = hasRole(requiredRole);
  } else if (requiredRoles && requiredRoles.length > 0) {
    hasRequiredRole = hasAnyRole(requiredRoles);
  }

  // Check permission requirements
  let hasRequiredPermission = true;
  if (requiredPermission) {
    hasRequiredPermission = hasPermission(requiredPermission);
  } else if (requiredPermissions && requiredPermissions.length > 0) {
    hasRequiredPermission = requireAllPermissions
      ? hasAllPermissions(requiredPermissions)
      : hasAnyPermission(requiredPermissions);
  }

  // If user lacks required role or permission, redirect to dashboard with toast
  useEffect(() => {
    if ((!hasRequiredRole || !hasRequiredPermission) && !toastShownRef.current) {
      toastShownRef.current = true;
      toast({
        title: 'Access Denied',
        description: 'You do not have permission to access this page.',
        variant: 'destructive',
      });
    }
  }, [hasRequiredRole, hasRequiredPermission, toast]);

  if (!hasRequiredRole || !hasRequiredPermission) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

/**
 * Simple authentication-only protected route
 */
export function AuthenticatedRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}
