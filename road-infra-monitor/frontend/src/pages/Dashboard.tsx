import { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Progress } from '@/components/ui/progress';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { StatusBadge, SparklineChart } from '@/components/shared';
import { cn, formatDateTime } from '@/lib/utils';
import {
  Building2,
  AlertTriangle,
  Activity,
  CheckCircle,
  XCircle,
  Clock,
  Radio,
  TrendingUp,
  TrendingDown,
  ArrowRight,
  Zap,
  MapPin,
  Bell,
  RefreshCw,
} from 'lucide-react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  AreaChart,
  Area,
} from 'recharts';
import { assetService, sensorService, alertService, analyticsService } from '@/services';
import { format } from 'date-fns';

const HEALTH_COLORS: Record<string, string> = {
  // Backend returns display names (e.g., "Healthy") via @JsonValue
  Healthy: '#27AE60',
  Fair: '#3498DB',
  Warning: '#F39C12',
  Critical: '#E74C3C',
  Offline: '#95A5A6',
  Unknown: '#6B7280',
  // Fallback for uppercase keys (just in case)
  HEALTHY: '#27AE60',
  FAIR: '#3498DB',
  WARNING: '#F39C12',
  CRITICAL: '#E74C3C',
  OFFLINE: '#95A5A6',
  UNKNOWN: '#6B7280',
};

interface StatCard {
  title: string;
  value: number;
  change?: number;
  changeLabel?: string;
  icon: React.ElementType;
  color: string;
  bgColor: string;
  trend?: number[];
}

type TimeframeOption = '1h' | '6h' | '12h' | '24h';

const TIMEFRAME_CONFIG: Record<TimeframeOption, { label: string; hours: number; interval: number }> = {
  '1h': { label: '1 Hour', hours: 1, interval: 5 },      // 5-minute intervals
  '6h': { label: '6 Hours', hours: 6, interval: 30 },    // 30-minute intervals
  '12h': { label: '12 Hours', hours: 12, interval: 60 }, // 1-hour intervals
  '24h': { label: '24 Hours', hours: 24, interval: 60 }, // 1-hour intervals
};

export default function Dashboard() {
  const navigate = useNavigate();
  const [systemHealth, setSystemHealth] = useState(92);
  const [timeframe, setTimeframe] = useState<TimeframeOption>('24h');

  // Queries - with retry:false to prevent logout loops on API errors
  const { data: assetsData, isLoading: assetsLoading, isError: assetsError } = useQuery({
    queryKey: ['dashboardAssets'],
    queryFn: () => assetService.getAll(0, 100),
    retry: false,
  });

  const { data: sensorsData, isLoading: sensorsLoading, isError: sensorsError } = useQuery({
    queryKey: ['dashboardSensors'],
    queryFn: () => sensorService.getAll(0, 100),
    retry: false,
  });

  const { data: alertsData, isLoading: alertsLoading, isError: alertsError } = useQuery({
    queryKey: ['dashboardAlerts'],
    queryFn: () => alertService.getFiltered({ status: ['OPEN', 'ACKNOWLEDGED', 'IN_PROGRESS', 'ESCALATED'], page: 0, size: 10 }),
    retry: false,
  });

  // Process data
  const assets = assetsData?.content || [];
  const sensors = sensorsData?.content || [];
  const alerts = alertsData?.content || [];

  const healthDistribution = assets.reduce((acc: Record<string, number>, asset: any) => {
    const status = asset.healthStatus || 'UNKNOWN';
    acc[status] = (acc[status] || 0) + 1;
    return acc;
  }, {});

  const sensorStats = {
    total: sensors.length,
    // Backend SensorStatus: Active, Inactive, Maintenance, Faulty, Decommissioned, Offline
    online: sensors.filter((s: any) => s.status === 'Active' || s.active === true).length,
    warning: sensors.filter((s: any) => s.status === 'Maintenance' || s.status === 'Faulty').length,
    offline: sensors.filter((s: any) => s.status === 'Offline' || s.status === 'Inactive' || s.status === 'Decommissioned').length,
  };

  const pieData = Object.entries(healthDistribution).map(([name, value]) => ({
    name,
    value: value as number,
    color: HEALTH_COLORS[name] || HEALTH_COLORS['Unknown'] || '#6B7280',
  }));

  // Mock trend data
  const mockTrend = Array.from({ length: 7 }, () => 80 + Math.random() * 20);

  const stats: StatCard[] = [
    {
      title: 'Total Assets',
      value: assets.length || 24,
      change: 2,
      changeLabel: 'from last month',
      icon: Building2,
      color: 'text-primary',
      bgColor: 'bg-primary/10',
      trend: mockTrend,
    },
    {
      title: 'Active Sensors',
      value: sensorStats.online || 156,
      change: -3,
      changeLabel: 'from yesterday',
      icon: Radio,
      color: 'text-green-500',
      bgColor: 'bg-green-500/10',
      trend: mockTrend.map(v => v + Math.random() * 10),
    },
    {
      title: 'Active Alerts',
      value: alerts.length || 8,
      change: 5,
      changeLabel: 'new today',
      icon: Bell,
      color: 'text-yellow-500',
      bgColor: 'bg-yellow-500/10',
      trend: mockTrend.map(v => 100 - v + Math.random() * 20),
    },
    {
      title: 'System Health',
      value: systemHealth,
      change: 1.2,
      changeLabel: 'vs. last week',
      icon: Activity,
      color: 'text-emerald-500',
      bgColor: 'bg-emerald-500/10',
      trend: mockTrend,
    },
  ];

  // Sensor activity data for chart based on selected timeframe
  const sensorActivityData = useMemo(() => {
    const config = TIMEFRAME_CONFIG[timeframe];
    const dataPoints = Math.ceil((config.hours * 60) / config.interval);
    
    return Array.from({ length: dataPoints }, (_, i) => {
      const minutesAgo = (dataPoints - 1 - i) * config.interval;
      const timestamp = new Date(Date.now() - minutesAgo * 60 * 1000);
      
      // Format time based on timeframe
      const timeFormat = config.hours <= 6 ? 'HH:mm' : 'HH:mm';
      
      return {
        time: format(timestamp, timeFormat),
        readings: 150 + Math.floor(Math.random() * 50),
        alerts: Math.floor(Math.random() * 5),
      };
    });
  }, [timeframe]);

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Dashboard</h1>
          <p className="text-muted-foreground">
            Real-time overview of road infrastructure monitoring
          </p>
        </div>
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <RefreshCw className="h-4 w-4" />
          Last updated: {format(new Date(), 'HH:mm:ss')}
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <Card key={stat.title} className="relative overflow-hidden">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                {stat.title}
              </CardTitle>
              <div className={cn('p-2 rounded-lg', stat.bgColor)}>
                <stat.icon className={cn('h-4 w-4', stat.color)} />
              </div>
            </CardHeader>
            <CardContent>
              <div className="flex items-end justify-between">
                <div>
                  <div className="text-3xl font-bold">
                    {stat.title === 'System Health' ? `${stat.value}%` : stat.value}
                  </div>
                  {stat.change !== undefined && (
                    <div className={cn(
                      'flex items-center gap-1 text-xs mt-1',
                      stat.change >= 0 ? 'text-green-500' : 'text-red-500'
                    )}>
                      {stat.change >= 0 ? (
                        <TrendingUp className="h-3 w-3" />
                      ) : (
                        <TrendingDown className="h-3 w-3" />
                      )}
                      <span>
                        {stat.change >= 0 ? '+' : ''}{stat.change} {stat.changeLabel}
                      </span>
                    </div>
                  )}
                </div>
                {stat.trend && (
                  <div className="h-12 w-24">
                    <SparklineChart data={stat.trend} color={stat.color.replace('text-', '')} />
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Main Content Row */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Sensor Activity Chart */}
        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between">
            <div>
              <CardTitle>Sensor Activity</CardTitle>
              <CardDescription>
                Readings and alerts over the last {TIMEFRAME_CONFIG[timeframe].label.toLowerCase()}
              </CardDescription>
            </div>
            <div className="flex items-center gap-2">
              <Select value={timeframe} onValueChange={(v) => setTimeframe(v as TimeframeOption)}>
                <SelectTrigger className="w-[120px]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="1h">1 Hour</SelectItem>
                  <SelectItem value="6h">6 Hours</SelectItem>
                  <SelectItem value="12h">12 Hours</SelectItem>
                  <SelectItem value="24h">24 Hours</SelectItem>
                </SelectContent>
              </Select>
              <Button variant="outline" size="sm" onClick={() => navigate('/sensors')}>
                View All
                <ArrowRight className="h-4 w-4 ml-1" />
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={sensorActivityData}>
                  <defs>
                    <linearGradient id="colorReadings" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#1B4F72" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="#1B4F72" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                  <XAxis 
                    dataKey="time" 
                    stroke="#6b7280" 
                    fontSize={12}
                    label={{ value: 'Time', position: 'insideBottom', offset: -5, fill: '#6b7280' }}
                  />
                  <YAxis 
                    stroke="#6b7280" 
                    fontSize={12}
                    label={{ value: 'Count', angle: -90, position: 'insideLeft', fill: '#6b7280' }}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#fff',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="readings"
                    stroke="#1B4F72"
                    strokeWidth={2}
                    fill="url(#colorReadings)"
                    name="Readings"
                  />
                  <Line
                    type="monotone"
                    dataKey="alerts"
                    stroke="#E74C3C"
                    strokeWidth={2}
                    dot={false}
                    name="Alerts"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        {/* Health Distribution */}
        <Card>
          <CardHeader>
            <CardTitle>Asset Health</CardTitle>
            <CardDescription>Distribution by status</CardDescription>
          </CardHeader>
          <CardContent>
            {assetsLoading ? (
              <Skeleton className="h-[200px]" />
            ) : pieData.length === 0 ? (
              <div className="h-[200px] flex items-center justify-center text-muted-foreground">
                No asset data available
              </div>
            ) : (
              <>
                <div className="h-[200px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={pieData}
                        cx="50%"
                        cy="50%"
                        innerRadius={50}
                        outerRadius={80}
                        paddingAngle={2}
                        dataKey="value"
                      >
                        {pieData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <div className="mt-4 space-y-2">
                  {pieData.map((item) => (
                    <div key={item.name} className="flex items-center justify-between text-sm">
                      <div className="flex items-center gap-2">
                        <div
                          className="h-3 w-3 rounded-full"
                          style={{ backgroundColor: item.color }}
                        />
                        <span>{item.name}</span>
                      </div>
                      <span className="font-medium">{item.value}</span>
                    </div>
                  ))}
                </div>
              </>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Bottom Row */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Active Alerts */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <div>
              <CardTitle>Active Alerts</CardTitle>
              <CardDescription>Recent alerts requiring attention</CardDescription>
            </div>
            <Button variant="outline" size="sm" onClick={() => navigate('/alerts')}>
              View All
              <ArrowRight className="h-4 w-4 ml-1" />
            </Button>
          </CardHeader>
          <CardContent>
            {alertsLoading ? (
              <div className="space-y-4">
                {[...Array(3)].map((_, i) => (
                  <Skeleton key={i} className="h-16" />
                ))}
              </div>
            ) : alerts.length === 0 ? (
              <div className="flex h-[200px] flex-col items-center justify-center text-muted-foreground">
                <CheckCircle className="h-12 w-12 text-green-500 mb-2" />
                <p>No active alerts</p>
                <p className="text-sm">All systems operating normally</p>
              </div>
            ) : (
              <div className="space-y-3 max-h-[280px] overflow-auto scrollbar-thin">
                {alerts.slice(0, 5).map((alert: any) => (
                  <div
                    key={alert.id}
                    className="flex items-start gap-3 rounded-lg border p-3 hover:bg-accent/50 transition-colors cursor-pointer"
                    onClick={() => navigate(`/alerts?id=${alert.id}`)}
                  >
                    <div className={cn(
                      'flex h-8 w-8 items-center justify-center rounded-full',
                      alert.severity === 'CRITICAL' ? 'bg-red-100 text-red-500' :
                      alert.severity === 'HIGH' ? 'bg-orange-100 text-orange-500' :
                      alert.severity === 'MEDIUM' ? 'bg-yellow-100 text-yellow-500' :
                      'bg-blue-100 text-blue-500'
                    )}>
                      <AlertTriangle className="h-4 w-4" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium truncate">{alert.title || 'Alert'}</p>
                      <p className="text-sm text-muted-foreground truncate">
                        {alert.message || alert.description}
                      </p>
                      <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                        <Clock className="h-3 w-3" />
                        {format(new Date(alert.createdAt || alert.timestamp), 'MMM d, HH:mm')}
                      </div>
                    </div>
                    <Badge
                      variant={alert.severity === 'CRITICAL' ? 'destructive' : 'secondary'}
                      className="shrink-0"
                    >
                      {alert.severity}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Sensor Status */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <div>
              <CardTitle>Sensor Status</CardTitle>
              <CardDescription>Overview of sensor network health</CardDescription>
            </div>
            <Button variant="outline" size="sm" onClick={() => navigate('/map')}>
              View Map
              <MapPin className="h-4 w-4 ml-1" />
            </Button>
          </CardHeader>
          <CardContent>
            {sensorsLoading ? (
              <Skeleton className="h-[200px]" />
            ) : (
              <div className="space-y-6">
                {/* Status bars */}
                <div className="space-y-4">
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm flex items-center gap-2">
                        <div className="h-2 w-2 rounded-full bg-green-500" />
                        Online
                      </span>
                      <span className="text-sm font-medium">
                        {sensorStats.online} ({Math.round((sensorStats.online / Math.max(sensorStats.total, 1)) * 100)}%)
                      </span>
                    </div>
                    <Progress value={(sensorStats.online / Math.max(sensorStats.total, 1)) * 100} className="h-2" />
                  </div>
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm flex items-center gap-2">
                        <div className="h-2 w-2 rounded-full bg-yellow-500" />
                        Warning
                      </span>
                      <span className="text-sm font-medium">
                        {sensorStats.warning} ({Math.round((sensorStats.warning / Math.max(sensorStats.total, 1)) * 100)}%)
                      </span>
                    </div>
                    <Progress value={(sensorStats.warning / Math.max(sensorStats.total, 1)) * 100} className="h-2 [&>div]:bg-yellow-500" />
                  </div>
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm flex items-center gap-2">
                        <div className="h-2 w-2 rounded-full bg-gray-500" />
                        Offline
                      </span>
                      <span className="text-sm font-medium">
                        {sensorStats.offline} ({Math.round((sensorStats.offline / Math.max(sensorStats.total, 1)) * 100)}%)
                      </span>
                    </div>
                    <Progress value={(sensorStats.offline / Math.max(sensorStats.total, 1)) * 100} className="h-2 [&>div]:bg-gray-500" />
                  </div>
                </div>

                {/* Quick stats */}
                <div className="grid grid-cols-3 gap-4 pt-4 border-t">
                  <div className="text-center">
                    <div className="text-2xl font-bold text-primary">{sensorStats.total || 168}</div>
                    <div className="text-xs text-muted-foreground">Total Sensors</div>
                  </div>
                  <div className="text-center">
                    <div className="text-2xl font-bold text-green-500">
                      {Math.round((sensorStats.online / Math.max(sensorStats.total, 1)) * 100) || 93}%
                    </div>
                    <div className="text-xs text-muted-foreground">Uptime</div>
                  </div>
                  <div className="text-center">
                    <div className="text-2xl font-bold text-blue-500">4.2M</div>
                    <div className="text-xs text-muted-foreground">Readings/Day</div>
                  </div>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
