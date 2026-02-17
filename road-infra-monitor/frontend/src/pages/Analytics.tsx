import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { formatNumber, formatPercentage } from '@/lib/utils';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
} from 'recharts';
import { TrendingUp, TrendingDown, Minus, Target, AlertTriangle } from 'lucide-react';

// Mock data for demonstration
const mockKpis = [
  {
    id: '1',
    metricName: 'average_health_score',
    displayName: 'Average Health Score',
    value: 87.5,
    previousValue: 85.2,
    targetValue: 90,
    unit: '%',
    percentageChange: 2.7,
    trend: 'UP',
    onTarget: false,
    category: 'health',
  },
  {
    id: '2',
    metricName: 'critical_assets',
    displayName: 'Critical Assets',
    value: 3,
    previousValue: 5,
    targetValue: 0,
    percentageChange: -40,
    trend: 'DOWN',
    onTarget: false,
    category: 'health',
  },
  {
    id: '3',
    metricName: 'sensor_uptime',
    displayName: 'Sensor Uptime',
    value: 99.2,
    previousValue: 98.8,
    targetValue: 99,
    unit: '%',
    percentageChange: 0.4,
    trend: 'UP',
    onTarget: true,
    category: 'performance',
  },
  {
    id: '4',
    metricName: 'alerts_resolved',
    displayName: 'Alerts Resolved',
    value: 156,
    previousValue: 142,
    targetValue: 150,
    percentageChange: 9.9,
    trend: 'UP',
    onTarget: true,
    category: 'operations',
  },
];

const healthTrendData = [
  { date: 'Jan', score: 82 },
  { date: 'Feb', score: 84 },
  { date: 'Mar', score: 83 },
  { date: 'Apr', score: 86 },
  { date: 'May', score: 85 },
  { date: 'Jun', score: 88 },
];

const assetTypeData = [
  { name: 'Bridges', healthy: 45, warning: 8, critical: 2 },
  { name: 'Tunnels', healthy: 12, warning: 3, critical: 1 },
  { name: 'Road Segments', healthy: 78, warning: 15, critical: 3 },
  { name: 'Culverts', healthy: 34, warning: 6, critical: 0 },
];

export default function Analytics() {
  const isLoading = false;

  const getTrendIcon = (trend: string) => {
    switch (trend) {
      case 'UP':
        return <TrendingUp className="h-4 w-4 text-green-500" />;
      case 'DOWN':
        return <TrendingDown className="h-4 w-4 text-red-500" />;
      default:
        return <Minus className="h-4 w-4 text-gray-500" />;
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Analytics</h2>
        <p className="text-muted-foreground">
          Key performance indicators and trends
        </p>
      </div>

      {/* KPI Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {mockKpis.map((kpi) => (
          <Card key={kpi.id}>
            <CardHeader className="pb-2">
              <CardDescription className="flex items-center justify-between">
                {kpi.displayName}
                {kpi.onTarget ? (
                  <Target className="h-4 w-4 text-green-500" />
                ) : (
                  <AlertTriangle className="h-4 w-4 text-yellow-500" />
                )}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex items-baseline gap-2">
                <span className="text-2xl font-bold">
                  {formatNumber(kpi.value, kpi.unit === '%' ? 1 : 0)}
                </span>
                {kpi.unit && <span className="text-muted-foreground">{kpi.unit}</span>}
              </div>
              <div className="mt-2 flex items-center gap-2">
                {getTrendIcon(kpi.trend)}
                <span
                  className={`text-sm ${
                    kpi.percentageChange > 0
                      ? 'text-green-500'
                      : kpi.percentageChange < 0
                      ? 'text-red-500'
                      : 'text-gray-500'
                  }`}
                >
                  {formatPercentage(kpi.percentageChange)}
                </span>
                <span className="text-sm text-muted-foreground">vs last period</span>
              </div>
              {kpi.targetValue !== undefined && (
                <div className="mt-2 text-sm text-muted-foreground">
                  Target: {kpi.targetValue}
                  {kpi.unit}
                </div>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        {/* Health Score Trend */}
        <Card>
          <CardHeader>
            <CardTitle>Health Score Trend</CardTitle>
            <CardDescription>Average health score over time</CardDescription>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={healthTrendData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis domain={[75, 100]} />
                <Tooltip />
                <Line
                  type="monotone"
                  dataKey="score"
                  stroke="hsl(var(--primary))"
                  strokeWidth={2}
                  dot={{ fill: 'hsl(var(--primary))' }}
                />
              </LineChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* Asset Health by Type */}
        <Card>
          <CardHeader>
            <CardTitle>Asset Health by Type</CardTitle>
            <CardDescription>Distribution of health status</CardDescription>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={assetTypeData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Bar dataKey="healthy" stackId="a" fill="#22c55e" name="Healthy" />
                <Bar dataKey="warning" stackId="a" fill="#eab308" name="Warning" />
                <Bar dataKey="critical" stackId="a" fill="#ef4444" name="Critical" />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>

      {/* Off-Target KPIs */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-yellow-500" />
            Off-Target KPIs
          </CardTitle>
          <CardDescription>KPIs that need attention</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {mockKpis
              .filter((kpi) => !kpi.onTarget)
              .map((kpi) => (
                <div
                  key={kpi.id}
                  className="flex items-center justify-between rounded-lg border p-4"
                >
                  <div>
                    <p className="font-medium">{kpi.displayName}</p>
                    <p className="text-sm text-muted-foreground">
                      Current: {kpi.value}
                      {kpi.unit} | Target: {kpi.targetValue}
                      {kpi.unit}
                    </p>
                  </div>
                  <Badge variant="warning">
                    {Math.abs(((kpi.value - (kpi.targetValue || 0)) / (kpi.targetValue || 1)) * 100).toFixed(1)}%
                    {kpi.value < (kpi.targetValue || 0) ? ' below' : ' above'} target
                  </Badge>
                </div>
              ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
