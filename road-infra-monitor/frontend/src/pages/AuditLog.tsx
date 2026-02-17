import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { ScrollArea } from '@/components/ui/scroll-area';
import { TimeRangeSelector, EmptyState, getDateRangeFromPreset, type TimeRange } from '@/components/shared';
import {
  Search,
  FileText,
  Download,
  RefreshCw,
  Eye,
  User,
  Calendar,
  Monitor,
  Activity,
  Shield,
  AlertCircle,
  CheckCircle,
  XCircle,
  Info,
} from 'lucide-react';
import { auditService } from '@/services';
import type { AuditLog } from '@/types';
import { format } from 'date-fns';

const ACTION_TYPES = [
  'LOGIN',
  'LOGOUT',
  'CREATE',
  'UPDATE',
  'DELETE',
  'VIEW',
  'EXPORT',
  'CONFIGURE',
];

const ENTITY_TYPES = [
  'USER',
  'SENSOR',
  'ASSET',
  'ALERT',
  'REPORT',
  'SETTING',
];

const getActionIcon = (action: string) => {
  switch (action) {
    case 'LOGIN':
      return <CheckCircle className="h-4 w-4 text-green-500" />;
    case 'LOGOUT':
      return <XCircle className="h-4 w-4 text-gray-500" />;
    case 'CREATE':
      return <CheckCircle className="h-4 w-4 text-blue-500" />;
    case 'UPDATE':
      return <Activity className="h-4 w-4 text-yellow-500" />;
    case 'DELETE':
      return <XCircle className="h-4 w-4 text-red-500" />;
    case 'VIEW':
      return <Eye className="h-4 w-4 text-purple-500" />;
    case 'EXPORT':
      return <Download className="h-4 w-4 text-blue-500" />;
    case 'CONFIGURE':
      return <Shield className="h-4 w-4 text-orange-500" />;
    default:
      return <Info className="h-4 w-4 text-muted-foreground" />;
  }
};

const getActionBadgeVariant = (action: string): 'default' | 'secondary' | 'destructive' | 'outline' => {
  switch (action) {
    case 'DELETE':
      return 'destructive';
    case 'CREATE':
    case 'LOGIN':
      return 'default';
    case 'UPDATE':
    case 'CONFIGURE':
      return 'secondary';
    default:
      return 'outline';
  }
};

export default function AuditLogPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [actionFilter, setActionFilter] = useState<string>('all');
  const [entityFilter, setEntityFilter] = useState<string>('all');
  const [timeRange, setTimeRange] = useState<TimeRange>('7d');
  const [page, setPage] = useState(0);
  const [detailsOpen, setDetailsOpen] = useState(false);
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);

  // Get date range from preset
  const dateRange = getDateRangeFromPreset(timeRange);

  // Query
  const { data: logsData, isLoading, refetch } = useQuery({
    queryKey: ['auditLogs', searchQuery, actionFilter, entityFilter, timeRange, page],
    queryFn: () => auditService.listAuditLogs({
      userId: searchQuery || undefined,
      action: actionFilter !== 'all' ? actionFilter : undefined,
      resourceType: entityFilter !== 'all' ? entityFilter : undefined,
      startDate: dateRange.start.toISOString(),
      endDate: dateRange.end.toISOString(),
      page,
      size: 20,
    }),
    select: (res) => ({
      logs: res?.content || [],
      totalPages: res?.totalPages || 1,
      totalElements: res?.totalElements || 0,
    }),
  });

  const logs = logsData?.logs || [];
  const totalPages = logsData?.totalPages || 1;
  const totalElements = logsData?.totalElements || 0;

  const handleExport = async () => {
    try {
      const response = await auditService.exportAuditLogs({
        userId: searchQuery || undefined,
        action: actionFilter !== 'all' ? actionFilter : undefined,
        resourceType: entityFilter !== 'all' ? entityFilter : undefined,
        startDate: dateRange.start.toISOString(),
        endDate: dateRange.end.toISOString(),
      });
      
      // Create download link
      const url = window.URL.createObjectURL(response);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `audit-log-${format(new Date(), 'yyyy-MM-dd')}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error('Failed to export audit log:', error);
    }
  };

  const viewDetails = (log: AuditLog) => {
    setSelectedLog(log);
    setDetailsOpen(true);
  };

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Audit Log</h1>
          <p className="text-muted-foreground">
            Track all system activities and user actions
          </p>
        </div>
        <Button variant="outline" onClick={handleExport}>
          <Download className="h-4 w-4 mr-2" />
          Export CSV
        </Button>
      </div>

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Events</CardTitle>
            <FileText className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalElements.toLocaleString()}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Today's Logins</CardTitle>
            <User className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {logs.filter((l: AuditLog) => l.action === 'LOGIN').length}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Modifications</CardTitle>
            <Activity className="h-4 w-4 text-yellow-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {logs.filter((l: AuditLog) => ['CREATE', 'UPDATE', 'DELETE'].includes(l.action)).length}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Security Events</CardTitle>
            <Shield className="h-4 w-4 text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {logs.filter((l: AuditLog) => l.action === 'CONFIGURE').length}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search by username..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9"
              />
            </div>
            <Select value={actionFilter} onValueChange={setActionFilter}>
              <SelectTrigger className="w-[150px]">
                <SelectValue placeholder="All Actions" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Actions</SelectItem>
                {ACTION_TYPES.map((action) => (
                  <SelectItem key={action} value={action}>
                    {action}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select value={entityFilter} onValueChange={setEntityFilter}>
              <SelectTrigger className="w-[150px]">
                <SelectValue placeholder="All Entities" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Entities</SelectItem>
                {ENTITY_TYPES.map((entity) => (
                  <SelectItem key={entity} value={entity}>
                    {entity}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <TimeRangeSelector
              value={timeRange}
              onChange={(range) => setTimeRange(range)}
            />
            <Button variant="outline" size="icon" onClick={() => refetch()}>
              <RefreshCw className="h-4 w-4" />
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Audit Log Table */}
      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="flex items-center justify-center h-64">
              <RefreshCw className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : logs.length === 0 ? (
            <EmptyState
              icon="file"
              title="No audit logs found"
              description="No events match your current filters."
            />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-[180px]">Timestamp</TableHead>
                    <TableHead>User</TableHead>
                    <TableHead>Action</TableHead>
                    <TableHead>Entity</TableHead>
                    <TableHead>Description</TableHead>
                    <TableHead>IP Address</TableHead>
                    <TableHead className="w-[70px]"></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {logs.map((log: AuditLog) => (
                    <TableRow key={log.id}>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Calendar className="h-4 w-4 text-muted-foreground" />
                          <span className="text-sm">
                            {format(new Date(log.timestamp), 'MMM d, yyyy HH:mm:ss')}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-muted text-sm font-semibold">
                            {log.username.slice(0, 2).toUpperCase()}
                          </div>
                          <span className="font-medium">{log.username}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          {getActionIcon(log.action)}
                          <Badge variant={getActionBadgeVariant(log.action)}>
                            {log.action}
                          </Badge>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline">{log.entityType}</Badge>
                        {log.entityId && (
                          <span className="ml-2 text-xs text-muted-foreground">
                            #{log.entityId.slice(0, 8)}
                          </span>
                        )}
                      </TableCell>
                      <TableCell className="max-w-[300px] truncate">
                        {log.description}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2 text-sm text-muted-foreground">
                          <Monitor className="h-4 w-4" />
                          {log.ipAddress || '-'}
                        </div>
                      </TableCell>
                      <TableCell>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => viewDetails(log)}
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {/* Pagination */}
              <div className="flex items-center justify-between px-4 py-3 border-t">
                <div className="text-sm text-muted-foreground">
                  Showing {logs.length} of {totalElements} events
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                  >
                    Previous
                  </Button>
                  <span className="text-sm">
                    Page {page + 1} of {totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => p + 1)}
                    disabled={page >= totalPages - 1}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {/* Details Dialog */}
      <Dialog open={detailsOpen} onOpenChange={setDetailsOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Audit Log Details</DialogTitle>
          </DialogHeader>
          {selectedLog && (
            <ScrollArea className="max-h-[60vh]">
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <p className="text-sm text-muted-foreground">Timestamp</p>
                    <p className="font-medium">
                      {format(new Date(selectedLog.timestamp), 'MMMM d, yyyy HH:mm:ss')}
                    </p>
                  </div>
                  <div className="space-y-1">
                    <p className="text-sm text-muted-foreground">User</p>
                    <p className="font-medium">{selectedLog.username}</p>
                  </div>
                  <div className="space-y-1">
                    <p className="text-sm text-muted-foreground">Action</p>
                    <div className="flex items-center gap-2">
                      {getActionIcon(selectedLog.action)}
                      <Badge variant={getActionBadgeVariant(selectedLog.action)}>
                        {selectedLog.action}
                      </Badge>
                    </div>
                  </div>
                  <div className="space-y-1">
                    <p className="text-sm text-muted-foreground">Entity</p>
                    <div className="flex items-center gap-2">
                      <Badge variant="outline">{selectedLog.entityType}</Badge>
                      {selectedLog.entityId && (
                        <span className="text-sm text-muted-foreground">
                          {selectedLog.entityId}
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="space-y-1">
                    <p className="text-sm text-muted-foreground">IP Address</p>
                    <p className="font-medium">{selectedLog.ipAddress || '-'}</p>
                  </div>
                  <div className="space-y-1">
                    <p className="text-sm text-muted-foreground">User Agent</p>
                    <p className="font-medium text-sm truncate">
                      {selectedLog.userAgent || '-'}
                    </p>
                  </div>
                </div>
                <div className="space-y-1">
                  <p className="text-sm text-muted-foreground">Description</p>
                  <p className="font-medium">{selectedLog.description}</p>
                </div>
                {selectedLog.details && (
                  <div className="space-y-1">
                    <p className="text-sm text-muted-foreground">Additional Details</p>
                    <pre className="bg-muted p-4 rounded-md text-sm overflow-auto">
                      {JSON.stringify(selectedLog.details, null, 2)}
                    </pre>
                  </div>
                )}
              </div>
            </ScrollArea>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
