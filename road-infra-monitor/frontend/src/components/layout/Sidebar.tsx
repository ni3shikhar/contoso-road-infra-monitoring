import { NavLink } from 'react-router-dom';
import { cn } from '@/lib/utils';
import {
  LayoutDashboard,
  Building2,
  Radio,
  Activity,
  Bell,
  BarChart3,
  Settings,
  ChevronLeft,
  Map,
  Users,
  FileText,
  Shield,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/store/authStore';
import type { Role, Permission } from '@/types';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';

interface SidebarProps {
  open: boolean;
  onToggle: () => void;
}

interface NavItem {
  name: string;
  href: string;
  icon: typeof LayoutDashboard;
  requiredRoles?: Role[];
  requiredPermission?: Permission;
}

const navigation: NavItem[] = [
  { name: 'Dashboard', href: '/', icon: LayoutDashboard },
  { name: 'Corridor Map', href: '/map', icon: Map },
  { name: 'Assets', href: '/assets', icon: Building2 },
  { name: 'Sensors', href: '/sensors', icon: Radio },
  { name: 'Monitoring', href: '/monitoring', icon: Activity },
  { name: 'Alerts', href: '/alerts', icon: Bell },
  { name: 'Analytics', href: '/analytics', icon: BarChart3 },
  { 
    name: 'User Management', 
    href: '/admin/users', 
    icon: Users,
    requiredRoles: ['ADMIN'],
  },
  { 
    name: 'Audit Log', 
    href: '/admin/audit', 
    icon: FileText,
    requiredRoles: ['ADMIN'],
  },
  { 
    name: 'Settings', 
    href: '/settings', 
    icon: Settings,
    requiredRoles: ['ADMIN', 'ENGINEER'],
  },
];

export default function Sidebar({ open, onToggle }: SidebarProps) {
  const hasAnyRole = useAuthStore((state) => state.hasAnyRole);
  const hasPermission = useAuthStore((state) => state.hasPermission);

  const isNavItemVisible = (item: NavItem) => {
    if (item.requiredRoles && !hasAnyRole(item.requiredRoles)) {
      return false;
    }
    if (item.requiredPermission && !hasPermission(item.requiredPermission)) {
      return false;
    }
    return true;
  };

  const visibleNavigation = navigation.filter(isNavItemVisible);

  return (
    <TooltipProvider delayDuration={0}>
      <aside
        className={cn(
          'flex flex-col border-r bg-card transition-sidebar',
          open ? 'w-64' : 'w-16'
        )}
      >
        {/* Logo and Toggle */}
        <div className="flex h-16 items-center justify-between border-b px-4">
          {open && (
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
                <Shield className="h-5 w-5 text-primary-foreground" />
              </div>
              <span className="text-lg font-semibold text-primary">
                Road Monitor
              </span>
            </div>
          )}
          {!open && (
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary mx-auto">
              <Shield className="h-5 w-5 text-primary-foreground" />
            </div>
          )}
          <Button
            variant="ghost"
            size="icon"
            onClick={onToggle}
            className={cn(!open && 'hidden')}
          >
            <ChevronLeft
              className={cn(
                'h-5 w-5 transition-transform',
                !open && 'rotate-180'
              )}
            />
          </Button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 space-y-1 p-2 overflow-y-auto scrollbar-thin">
          {visibleNavigation.map((item) => (
            <Tooltip key={item.name}>
              <TooltipTrigger asChild>
                <NavLink
                  to={item.href}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-primary text-primary-foreground'
                        : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                      !open && 'justify-center px-2'
                    )
                  }
                >
                  <item.icon className="h-5 w-5 flex-shrink-0" />
                  {open && <span>{item.name}</span>}
                </NavLink>
              </TooltipTrigger>
              {!open && (
                <TooltipContent side="right">
                  <p>{item.name}</p>
                </TooltipContent>
              )}
            </Tooltip>
          ))}
        </nav>

        {/* Collapse Toggle at Bottom (when collapsed) */}
        {!open && (
          <div className="border-t p-2">
            <Button
              variant="ghost"
              size="icon"
              onClick={onToggle}
              className="w-full"
            >
              <ChevronLeft className="h-5 w-5 rotate-180" />
            </Button>
          </div>
        )}
      </aside>
    </TooltipProvider>
  );
}
