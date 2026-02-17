import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { useEffect, useState } from 'react';
import type { User, Role, Permission, LoginResponse } from '@/types';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  mustChangePassword: boolean;
  
  // Actions
  setAuth: (response: LoginResponse) => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  logout: () => void;
  updateUser: (user: User) => void;
  setMustChangePassword: (value: boolean) => void;
  
  // RBAC helper methods
  hasPermission: (permission: Permission) => boolean;
  hasAnyPermission: (permissions: Permission[]) => boolean;
  hasAllPermissions: (permissions: Permission[]) => boolean;
  hasRole: (role: Role) => boolean;
  hasAnyRole: (roles: Role[]) => boolean;
  isAdmin: () => boolean;
  isEngineer: () => boolean;
  isOperator: () => boolean;
  isViewer: () => boolean;
  canManageUsers: () => boolean;
  canConfigureSensors: () => boolean;
  canManageAlerts: () => boolean;
  canExportData: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      mustChangePassword: false,
      
      setAuth: (response: LoginResponse) =>
        set({
          user: response.user,
          accessToken: response.accessToken,
          refreshToken: response.refreshToken,
          isAuthenticated: true,
          mustChangePassword: response.user.mustChangePassword ?? false,
        }),
        
      setTokens: (accessToken: string, refreshToken: string) =>
        set({ accessToken, refreshToken }),
        
      logout: () =>
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
          mustChangePassword: false,
        }),
        
      updateUser: (user: User) => set({ user }),
      
      setMustChangePassword: (value: boolean) => set({ mustChangePassword: value }),
      
      // RBAC Methods
      hasPermission: (permission: Permission) => {
        const user = get().user;
        if (!user) return false;
        return user.permissions?.includes(permission) ?? false;
      },
      
      hasAnyPermission: (permissions: Permission[]) => {
        const user = get().user;
        if (!user) return false;
        return permissions.some(p => user.permissions?.includes(p) ?? false);
      },
      
      hasAllPermissions: (permissions: Permission[]) => {
        const user = get().user;
        if (!user) return false;
        return permissions.every(p => user.permissions?.includes(p) ?? false);
      },
      
      hasRole: (role: Role) => {
        const user = get().user;
        if (!user) return false;
        return user.role === role;
      },
      
      hasAnyRole: (roles: Role[]) => {
        const user = get().user;
        if (!user) return false;
        return roles.includes(user.role);
      },
      
      isAdmin: () => get().hasRole('ADMIN'),
      isEngineer: () => get().hasRole('ENGINEER'),
      isOperator: () => get().hasRole('OPERATOR'),
      isViewer: () => get().hasRole('VIEWER'),
      
      canManageUsers: () => get().hasPermission('USER_MANAGE'),
      canConfigureSensors: () => get().hasPermission('SENSOR_CONFIGURE'),
      canManageAlerts: () => get().hasAnyPermission(['ALERT_ACKNOWLEDGE', 'ALERT_RESOLVE']),
      canExportData: () => get().hasPermission('ANALYTICS_EXPORT'),
    }),
    {
      name: 'road-infra-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
        mustChangePassword: state.mustChangePassword,
      }),
    }
  )
);

// Custom hook to track hydration state using React state
export const useAuthHydration = () => {
  const [hasHydrated, setHasHydrated] = useState(false);

  useEffect(() => {
    // Check if already hydrated
    const unsubFinishHydration = useAuthStore.persist.onFinishHydration(() => {
      setHasHydrated(true);
    });

    // Check if hydration already completed before subscription
    if (useAuthStore.persist.hasHydrated()) {
      setHasHydrated(true);
    }

    return () => {
      unsubFinishHydration();
    };
  }, []);

  return hasHydrated;
};
