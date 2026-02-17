import { Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from '@/components/ui/toaster';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Layout from '@/components/layout/Layout';
import { ProtectedRoute } from '@/components/auth';
import Dashboard from '@/pages/Dashboard';
import CorridorMap from '@/pages/CorridorMap';
import Assets from '@/pages/Assets';
import AssetDetail from '@/pages/AssetDetail';
import Sensors from '@/pages/Sensors';
import SensorDetail from '@/pages/SensorDetail';
import Monitoring from '@/pages/Monitoring';
import Alerts from '@/pages/Alerts';
import Analytics from '@/pages/Analytics';
import Settings from '@/pages/Settings';
import UserManagement from '@/pages/UserManagement';
import AuditLog from '@/pages/AuditLog';
import Login from '@/pages/Login';
import { useAuthStore, useAuthHydration } from '@/store/authStore';

// React Query client with sensible defaults
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function AuthenticatedRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const hasHydrated = useAuthHydration();
  
  // Wait for hydration from localStorage before checking auth
  if (!hasHydrated) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  
  return <>{children}</>;
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/*"
          element={
            <AuthenticatedRoute>
              <Layout>
                <Routes>
                  {/* Public routes (for authenticated users) */}
                  <Route path="/" element={<Dashboard />} />
                  <Route path="/map" element={<CorridorMap />} />
                  <Route path="/assets" element={<Assets />} />
                  <Route path="/assets/:id" element={<AssetDetail />} />
                  <Route path="/sensors" element={<Sensors />} />
                  <Route path="/sensors/:id" element={<SensorDetail />} />
                  <Route path="/monitoring" element={<Monitoring />} />
                  <Route path="/alerts" element={<Alerts />} />
                  <Route path="/analytics" element={<Analytics />} />
                  
                  {/* Admin-only routes */}
                  <Route
                    path="/admin/users"
                    element={
                      <ProtectedRoute requiredRoles={['ADMIN']}>
                        <UserManagement />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/admin/audit"
                    element={
                      <ProtectedRoute requiredRoles={['ADMIN']}>
                        <AuditLog />
                      </ProtectedRoute>
                    }
                  />
                  
                  {/* Admin/Engineer routes */}
                  <Route
                    path="/settings"
                    element={
                      <ProtectedRoute requiredRoles={['ADMIN', 'ENGINEER']}>
                        <Settings />
                      </ProtectedRoute>
                    }
                  />
                  
                  {/* Catch-all redirect */}
                  <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
              </Layout>
            </AuthenticatedRoute>
          }
        />
      </Routes>
      <Toaster />
    </QueryClientProvider>
  );
}

export default App;
