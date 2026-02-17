import { useState } from 'react';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
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
import { Skeleton } from '@/components/ui/skeleton';
import { useAlerts, useAcknowledgeAlert, useResolveAlert } from '@/hooks/useAlerts';
import { formatDateTime, getAlertSeverityColor } from '@/lib/utils';
import { Search, Check, CheckCheck } from 'lucide-react';

export default function Alerts() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const { data, isLoading } = useAlerts(page, 20);
  const acknowledgeMutation = useAcknowledgeAlert();
  const resolveMutation = useResolveAlert();

  const filteredAlerts = data?.content?.filter((alert) =>
    (alert?.title?.toLowerCase() || '').includes(search.toLowerCase()) ||
    (alert?.message?.toLowerCase() || '').includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Alerts</h2>
        <p className="text-muted-foreground">
          Monitor and manage system alerts
        </p>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center gap-4">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search alerts..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-4">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-16" />
              ))}
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Severity</TableHead>
                    <TableHead>Title</TableHead>
                    <TableHead>Asset</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Created</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredAlerts?.map((alert) => (
                    <TableRow key={alert.id}>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <div
                            className={`h-2 w-2 rounded-full ${getAlertSeverityColor(
                              alert.severity
                            )}`}
                          />
                          <Badge
                            variant={
                              alert.severity === 'CRITICAL'
                                ? 'destructive'
                                : alert.severity === 'HIGH'
                                ? 'destructive'
                                : 'secondary'
                            }
                          >
                            {alert.severity}
                          </Badge>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div>
                          <p className="font-medium">{alert.title}</p>
                          <p className="text-sm text-muted-foreground truncate max-w-[300px]">
                            {alert.message}
                          </p>
                        </div>
                      </TableCell>
                      <TableCell>{alert.assetName || '-'}</TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            alert.status === 'RESOLVED'
                              ? 'success'
                              : alert.status === 'ACKNOWLEDGED'
                              ? 'secondary'
                              : 'destructive'
                          }
                        >
                          {alert.status}
                        </Badge>
                      </TableCell>
                      <TableCell>{formatDateTime(alert.createdAt)}</TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          {alert.status === 'OPEN' && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => acknowledgeMutation.mutate(alert.id)}
                              disabled={acknowledgeMutation.isPending}
                            >
                              <Check className="h-4 w-4" />
                            </Button>
                          )}
                          {alert.status !== 'RESOLVED' && alert.status !== 'AUTO_RESOLVED' && alert.status !== 'DISMISSED' && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => resolveMutation.mutate(alert.id)}
                              disabled={resolveMutation.isPending}
                            >
                              <CheckCheck className="h-4 w-4" />
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              <div className="mt-4 flex items-center justify-between">
                <p className="text-sm text-muted-foreground">
                  Showing {(data?.number || 0) * 20 + 1} to{' '}
                  {Math.min((data?.number || 0) * 20 + 20, data?.totalElements || 0)} of{' '}
                  {data?.totalElements} alerts
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={data?.first}
                    onClick={() => setPage(page - 1)}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={data?.last}
                    onClick={() => setPage(page + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
