import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import type { Role } from '@/types';
import { Shield, Wrench, Settings, Eye } from 'lucide-react';

const roleConfig: Record<Role, { 
  label: string; 
  className: string; 
  icon: typeof Shield;
  description: string;
}> = {
  ADMIN: { 
    label: 'Admin', 
    className: 'bg-purple-500 hover:bg-purple-600 text-white',
    icon: Shield,
    description: 'Full system access'
  },
  ENGINEER: { 
    label: 'Engineer', 
    className: 'bg-blue-500 hover:bg-blue-600 text-white',
    icon: Wrench,
    description: 'Technical management'
  },
  OPERATOR: { 
    label: 'Operator', 
    className: 'bg-green-500 hover:bg-green-600 text-white',
    icon: Settings,
    description: 'Operations and monitoring'
  },
  VIEWER: { 
    label: 'Viewer', 
    className: 'bg-gray-500 hover:bg-gray-600 text-white',
    icon: Eye,
    description: 'Read-only access'
  },
};

interface RoleBadgeProps {
  role: Role;
  showIcon?: boolean;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export function RoleBadge({ role, showIcon = false, size = 'md', className }: RoleBadgeProps) {
  const config = roleConfig[role] || roleConfig.VIEWER;
  const Icon = config.icon;
  
  const sizeClasses = {
    sm: 'text-xs px-2 py-0.5',
    md: 'text-sm px-2.5 py-0.5',
    lg: 'text-base px-3 py-1',
  };
  
  const iconSizes = {
    sm: 'h-3 w-3',
    md: 'h-4 w-4',
    lg: 'h-5 w-5',
  };
  
  return (
    <Badge className={cn(config.className, sizeClasses[size], 'gap-1', className)}>
      {showIcon && <Icon className={iconSizes[size]} />}
      {config.label}
    </Badge>
  );
}

interface RoleInfoProps {
  role: Role;
}

export function RoleInfo({ role }: RoleInfoProps) {
  const config = roleConfig[role] || roleConfig.VIEWER;
  const Icon = config.icon;
  
  return (
    <div className="flex items-center gap-2">
      <div className={cn('p-2 rounded-full', config.className.split(' ')[0])}>
        <Icon className="h-4 w-4 text-white" />
      </div>
      <div>
        <p className="font-medium">{config.label}</p>
        <p className="text-sm text-muted-foreground">{config.description}</p>
      </div>
    </div>
  );
}

export function getRoleColor(role: Role): string {
  return roleConfig[role]?.className.split(' ')[0] || 'bg-gray-500';
}

export function getRoleLabel(role: Role): string {
  return roleConfig[role]?.label || role;
}
