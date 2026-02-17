import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Area,
  AreaChart,
} from 'recharts';
import { cn } from '@/lib/utils';

// ============================================
// Gauge Chart Component
// ============================================

interface GaugeChartProps {
  value: number;
  min?: number;
  max?: number;
  label?: string;
  size?: 'sm' | 'md' | 'lg';
  showValue?: boolean;
  className?: string;
}

export function GaugeChart({
  value,
  min = 0,
  max = 100,
  label,
  size = 'md',
  showValue = true,
  className,
}: GaugeChartProps) {
  const percentage = Math.min(Math.max((value - min) / (max - min), 0), 1) * 100;
  
  // Calculate color based on value
  const getColor = (pct: number) => {
    if (pct >= 80) return '#22c55e'; // green
    if (pct >= 60) return '#eab308'; // yellow
    if (pct >= 40) return '#f97316'; // orange
    return '#ef4444'; // red
  };
  
  const color = getColor(percentage);
  
  const sizeConfig = {
    sm: { width: 80, height: 50, strokeWidth: 8, fontSize: 14 },
    md: { width: 120, height: 70, strokeWidth: 10, fontSize: 18 },
    lg: { width: 160, height: 90, strokeWidth: 12, fontSize: 24 },
  };
  
  const config = sizeConfig[size];
  const radius = (config.width - config.strokeWidth) / 2;
  const circumference = Math.PI * radius;
  const offset = circumference - (percentage / 100) * circumference;
  
  return (
    <div className={cn('flex flex-col items-center', className)}>
      <svg
        width={config.width}
        height={config.height}
        viewBox={`0 0 ${config.width} ${config.height + 10}`}
      >
        {/* Background arc */}
        <path
          d={`M ${config.strokeWidth / 2} ${config.height} A ${radius} ${radius} 0 0 1 ${config.width - config.strokeWidth / 2} ${config.height}`}
          fill="none"
          stroke="#e5e7eb"
          strokeWidth={config.strokeWidth}
          strokeLinecap="round"
        />
        {/* Value arc */}
        <path
          d={`M ${config.strokeWidth / 2} ${config.height} A ${radius} ${radius} 0 0 1 ${config.width - config.strokeWidth / 2} ${config.height}`}
          fill="none"
          stroke={color}
          strokeWidth={config.strokeWidth}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          style={{ transition: 'stroke-dashoffset 0.5s ease' }}
        />
        {/* Value text */}
        {showValue && (
          <text
            x={config.width / 2}
            y={config.height - 5}
            textAnchor="middle"
            className="font-bold"
            style={{ fontSize: config.fontSize, fill: color }}
          >
            {Math.round(value)}
          </text>
        )}
      </svg>
      {label && (
        <span className="mt-1 text-sm text-muted-foreground">{label}</span>
      )}
    </div>
  );
}

// ============================================
// Sparkline Chart Component
// ============================================

interface SparklineChartProps {
  data: number[];
  width?: number;
  height?: number;
  color?: string;
  showArea?: boolean;
  className?: string;
}

export function SparklineChart({
  data,
  width = 100,
  height = 30,
  color = '#3b82f6',
  showArea = true,
  className,
}: SparklineChartProps) {
  const chartData = data.map((value, index) => ({ index, value }));
  
  return (
    <div className={cn('inline-block', className)} style={{ width, height }}>
      <ResponsiveContainer width="100%" height="100%">
        {showArea ? (
          <AreaChart data={chartData} margin={{ top: 2, right: 2, bottom: 2, left: 2 }}>
            <defs>
              <linearGradient id={`sparklineGradient-${color}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor={color} stopOpacity={0.3} />
                <stop offset="95%" stopColor={color} stopOpacity={0} />
              </linearGradient>
            </defs>
            <Area
              type="monotone"
              dataKey="value"
              stroke={color}
              strokeWidth={1.5}
              fill={`url(#sparklineGradient-${color})`}
              isAnimationActive={false}
            />
          </AreaChart>
        ) : (
          <LineChart data={chartData} margin={{ top: 2, right: 2, bottom: 2, left: 2 }}>
            <Line
              type="monotone"
              dataKey="value"
              stroke={color}
              strokeWidth={1.5}
              dot={false}
              isAnimationActive={false}
            />
          </LineChart>
        )}
      </ResponsiveContainer>
    </div>
  );
}

// ============================================
// Mini Line Chart for Cards
// ============================================

interface MiniLineChartProps {
  data: { label: string; value: number }[];
  color?: string;
  height?: number;
  className?: string;
}

export function MiniLineChart({
  data,
  color = '#3b82f6',
  height = 60,
  className,
}: MiniLineChartProps) {
  return (
    <div className={cn('w-full', className)} style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 5, right: 5, bottom: 5, left: 5 }}>
          <Line
            type="monotone"
            dataKey="value"
            stroke={color}
            strokeWidth={2}
            dot={false}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: 'hsl(var(--card))',
              border: '1px solid hsl(var(--border))',
              borderRadius: '6px',
              fontSize: '12px',
            }}
            labelStyle={{ color: 'hsl(var(--foreground))' }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

// ============================================
// Progress Bar Component
// ============================================

interface ProgressBarProps {
  value: number;
  max?: number;
  showLabel?: boolean;
  size?: 'sm' | 'md' | 'lg';
  color?: 'default' | 'success' | 'warning' | 'error';
  className?: string;
}

export function ProgressBar({
  value,
  max = 100,
  showLabel = false,
  size = 'md',
  color = 'default',
  className,
}: ProgressBarProps) {
  const percentage = Math.min(Math.max((value / max) * 100, 0), 100);
  
  const sizeClasses = {
    sm: 'h-1',
    md: 'h-2',
    lg: 'h-3',
  };
  
  const colorClasses = {
    default: 'bg-primary',
    success: 'bg-green-500',
    warning: 'bg-yellow-500',
    error: 'bg-red-500',
  };
  
  // Auto color based on percentage
  const autoColor = () => {
    if (percentage < 20) return 'bg-red-500';
    if (percentage < 50) return 'bg-yellow-500';
    return 'bg-green-500';
  };
  
  return (
    <div className={cn('flex items-center gap-2', className)}>
      <div className={cn('flex-1 rounded-full bg-muted', sizeClasses[size])}>
        <div
          className={cn(
            'rounded-full transition-all duration-300',
            sizeClasses[size],
            color === 'default' ? autoColor() : colorClasses[color]
          )}
          style={{ width: `${percentage}%` }}
        />
      </div>
      {showLabel && (
        <span className="text-sm font-medium">{Math.round(percentage)}%</span>
      )}
    </div>
  );
}

// ============================================
// Battery Level Indicator
// ============================================

interface BatteryLevelProps {
  level: number;
  size?: 'sm' | 'md';
  showLabel?: boolean;
  className?: string;
}

export function BatteryLevel({
  level,
  size = 'md',
  showLabel = true,
  className,
}: BatteryLevelProps) {
  const getColor = () => {
    if (level < 20) return 'bg-red-500';
    if (level < 50) return 'bg-yellow-500';
    return 'bg-green-500';
  };
  
  const sizeConfig = {
    sm: { width: 24, height: 12, borderWidth: 1, tipWidth: 2 },
    md: { width: 32, height: 16, borderWidth: 2, tipWidth: 3 },
  };
  
  const config = sizeConfig[size];
  
  return (
    <div className={cn('flex items-center gap-1', className)}>
      <div
        className="relative rounded-sm border-2 border-gray-400"
        style={{ width: config.width, height: config.height }}
      >
        <div
          className={cn('absolute left-0.5 top-0.5 bottom-0.5 rounded-sm', getColor())}
          style={{ width: `${Math.max(level - 5, 0)}%` }}
        />
        <div
          className="absolute -right-1 top-1/2 -translate-y-1/2 rounded-r-sm bg-gray-400"
          style={{ width: config.tipWidth, height: config.height / 2 }}
        />
      </div>
      {showLabel && (
        <span className={cn('text-xs', level < 20 && 'text-red-500')}>
          {level}%
        </span>
      )}
    </div>
  );
}
