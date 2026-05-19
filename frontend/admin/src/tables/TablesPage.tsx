import { useState, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { QRCodeSVG } from 'qrcode.react';
import { tableApi, type Table } from '../shared/api/adminApi';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../components/ui/dialog';
import { Switch } from '../components/ui/switch';
import { Badge } from '../components/ui/badge';
import { QrCode, Plus, RefreshCw, Trash2, Users, Copy, Check, Download, Eye, Clock, Timer, LayoutGrid, List } from 'lucide-react';
import { toast } from '../hooks/use-toast';
import { copyTextToClipboard } from '../shared/lib/clipboard';
import { FloorPlanView } from './FloorPlanView';

export function TablesPage() {
  const queryClient = useQueryClient();
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [selectedTable, setSelectedTable] = useState<Table | null>(null);
  const [qrTable, setQrTable] = useState<Table | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'list' | 'floor'>('list');
  const qrRef = useRef<HTMLDivElement>(null);

  const { data: tables = [], isLoading } = useQuery({
    queryKey: ['tables'],
    queryFn: tableApi.list,
  });

  const createMutation = useMutation({
    mutationFn: tableApi.create,
    onSuccess: (newTable) => {
      queryClient.invalidateQueries({ queryKey: ['tables'] });
      setShowAddDialog(false);
      setQrTable(newTable);
      toast({ title: 'Table Created', description: `Table ${newTable.tableNumber} has been created`, variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to create table', variant: 'destructive' }),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Parameters<typeof tableApi.update>[1] }) =>
      tableApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tables'] });
      setSelectedTable(null);
      toast({ title: 'Table Updated', description: 'Table settings have been saved', variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to update table', variant: 'destructive' }),
  });

  const deleteMutation = useMutation({
    mutationFn: tableApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tables'] });
      toast({ title: 'Table Deleted', description: 'Table has been removed', variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to delete table', variant: 'destructive' }),
  });

  const regenerateQrMutation = useMutation({
    mutationFn: tableApi.regenerateQr,
    onSuccess: (updatedTable) => {
      queryClient.invalidateQueries({ queryKey: ['tables'] });
      setQrTable(updatedTable);
      toast({ title: 'QR Code Regenerated', description: 'New QR code has been generated', variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to regenerate QR code', variant: 'destructive' }),
  });

  const sessionMutation = useMutation({
    mutationFn: async ({ id, action }: { id: string; action: 'create' | 'end' }): Promise<void> => {
      if (action === 'create') {
        await tableApi.createSession(id);
      } else {
        await tableApi.endSession(id);
      }
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['tables'] });
      toast({ 
        title: variables.action === 'create' ? 'Session Started' : 'Session Ended', 
        description: variables.action === 'create' ? 'New session has been started' : 'Session has been ended',
        variant: 'success' 
      });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to manage session', variant: 'destructive' }),
  });

  const extendSessionMutation = useMutation({
    mutationFn: ({ id, minutes }: { id: string; minutes: number }) =>
      tableApi.extendSession(id, minutes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tables'] });
      toast({ title: 'Session Extended', description: 'Session has been extended by 1 hour', variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to extend session', variant: 'destructive' }),
  });

  const formatRemainingTime = (minutes: number) => {
    if (minutes <= 0) return 'Expired';
    if (minutes < 60) return `${minutes}m`;
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  };

  const getTableUrl = (table: Table) => {
    const configured = (import.meta.env.VITE_MENU_URL as string | undefined)?.replace(/\/$/, '');
    const menuOrigin =
      configured ||
      (import.meta.env.DEV
        ? window.location.origin.replace(':5174', ':5173')
        : window.location.origin);
    return `${menuOrigin}/table/${table.qrCodeToken}`;
  };

  const copyQrUrl = async (table: Table) => {
    const url = getTableUrl(table);
    const ok = await copyTextToClipboard(url);
    if (ok) {
      setCopiedId(table.id);
      setTimeout(() => setCopiedId(null), 2000);
      toast({ title: 'URL copiada', description: 'Enlace del menú copiado al portapapeles', variant: 'success' });
    } else {
      toast({
        title: 'No se pudo copiar',
        description: 'Copiá la URL manualmente desde el cuadro de abajo.',
        variant: 'destructive',
      });
    }
  };

  const downloadQr = () => {
    if (!qrRef.current || !qrTable) return;
    
    const svg = qrRef.current.querySelector('svg');
    if (!svg) return;

    const svgData = new XMLSerializer().serializeToString(svg);
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    const img = new Image();
    
    img.onload = () => {
      canvas.width = 400;
      canvas.height = 480;
      
      if (ctx) {
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.drawImage(img, 50, 30, 300, 300);
        
        ctx.fillStyle = '#000000';
        ctx.font = 'bold 24px Inter, system-ui, sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(`Table ${qrTable.tableNumber}`, 200, 380);
        
        if (qrTable.tableName) {
          ctx.font = '16px Inter, system-ui, sans-serif';
          ctx.fillStyle = '#666666';
          ctx.fillText(qrTable.tableName, 200, 410);
        }
        
        ctx.font = '12px Inter, system-ui, sans-serif';
        ctx.fillStyle = '#999999';
        ctx.fillText('Scan to view menu', 200, 450);
      }

      const link = document.createElement('a');
      link.download = `table-${qrTable.tableNumber}-qr.png`;
      link.href = canvas.toDataURL('image/png');
      link.click();
    };

    img.src = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgData)));
  };

  const handleAddTable = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    createMutation.mutate({
      tableNumber: formData.get('tableNumber') as string,
      tableName: formData.get('tableName') as string || undefined,
      capacity: parseInt(formData.get('capacity') as string) || 4,
    });
  };

  const handleUpdateTable = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedTable) return;
    const formData = new FormData(e.currentTarget);
    updateMutation.mutate({
      id: selectedTable.id,
      data: {
        tableName: formData.get('tableName') as string || undefined,
        capacity: parseInt(formData.get('capacity') as string) || 4,
        active: formData.get('active') === 'on',
      },
    });
  };

  if (isLoading) {
    return <div className="flex items-center justify-center py-12">Loading...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Tables & QR Codes</h1>
          <p className="text-sm text-muted-foreground">
            Manage your restaurant tables and their QR codes
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex rounded-lg border bg-muted p-1">
            <Button
              variant={viewMode === 'list' ? 'default' : 'ghost'}
              size="sm"
              className="h-8"
              onClick={() => setViewMode('list')}
            >
              <List className="mr-2 h-4 w-4" />
              List
            </Button>
            <Button
              variant={viewMode === 'floor' ? 'default' : 'ghost'}
              size="sm"
              className="h-8"
              onClick={() => setViewMode('floor')}
            >
              <LayoutGrid className="mr-2 h-4 w-4" />
              Floor Plan
            </Button>
          </div>
          <Button onClick={() => setShowAddDialog(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Add Table
          </Button>
        </div>
      </div>

      {viewMode === 'floor' ? (
        <FloorPlanView 
          tables={tables} 
          onTableClick={(table) => setSelectedTable(table)}
        />
      ) : (
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {tables.map((table) => (
          <Card key={table.id} className={!table.active ? 'opacity-60' : ''}>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg">
                  Table {table.tableNumber}
                </CardTitle>
                <div className="flex items-center gap-2">
                  {table.hasActiveSession && (
                    <Badge variant="default" className="bg-green-500">
                      Session: {table.activeSessionCode}
                    </Badge>
                  )}
                  <Badge variant={table.active ? 'secondary' : 'outline'}>
                    {table.active ? 'Active' : 'Inactive'}
                  </Badge>
                </div>
              </div>
              {table.tableName && (
                <p className="text-sm text-muted-foreground">{table.tableName}</p>
              )}
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-4 text-sm text-muted-foreground">
                <div className="flex items-center gap-1">
                  <Users className="h-4 w-4" />
                  <span>{table.capacity} seats</span>
                </div>
              </div>

              {/* Session Info */}
              {table.hasActiveSession && (
                <div className={`rounded-lg p-3 ${table.sessionRemainingMinutes <= 30 ? 'bg-orange-50 border border-orange-200' : 'bg-green-50 border border-green-200'}`}>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Timer className={`h-4 w-4 ${table.sessionRemainingMinutes <= 30 ? 'text-orange-500' : 'text-green-500'}`} />
                      <span className={`text-sm font-medium ${table.sessionRemainingMinutes <= 30 ? 'text-orange-700' : 'text-green-700'}`}>
                        {formatRemainingTime(table.sessionRemainingMinutes)} remaining
                      </span>
                    </div>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="h-7 px-2 text-xs"
                      onClick={() => extendSessionMutation.mutate({ id: table.id, minutes: 60 })}
                      disabled={extendSessionMutation.isPending}
                    >
                      <Clock className="mr-1 h-3 w-3" />
                      +1h
                    </Button>
                  </div>
                  {table.sessionRemainingMinutes <= 30 && (
                    <p className="mt-1 text-xs text-orange-600">
                      Session expiring soon - extend or it will reset
                    </p>
                  )}
                </div>
              )}

              <div className="flex flex-wrap gap-2">
                <Button
                  size="sm"
                  variant="default"
                  onClick={() => setQrTable(table)}
                >
                  <Eye className="mr-1 h-3 w-3" />
                  View QR
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => copyQrUrl(table)}
                >
                  {copiedId === table.id ? (
                    <Check className="mr-1 h-3 w-3" />
                  ) : (
                    <Copy className="mr-1 h-3 w-3" />
                  )}
                  {copiedId === table.id ? 'Copied!' : 'Copy URL'}
                </Button>
                <Button
                  size="sm"
                  variant={table.hasActiveSession ? 'destructive' : 'secondary'}
                  onClick={() => {
                    if (table.hasActiveSession) {
                      if (confirm('End this session? Draft orders will be cancelled.')) {
                        sessionMutation.mutate({ id: table.id, action: 'end' });
                      }
                    } else {
                      sessionMutation.mutate({ id: table.id, action: 'create' });
                    }
                  }}
                >
                  {table.hasActiveSession ? 'End Session' : 'Start Session'}
                </Button>
              </div>

              <div className="flex gap-2 border-t pt-3">
                <Button
                  size="sm"
                  variant="ghost"
                  className="flex-1"
                  onClick={() => setSelectedTable(table)}
                >
                  Edit
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    if (confirm('Regenerate QR code? The old QR will stop working.')) {
                      regenerateQrMutation.mutate(table.id);
                    }
                  }}
                  disabled={regenerateQrMutation.isPending}
                >
                  <RefreshCw className="h-4 w-4" />
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  className="text-destructive hover:text-destructive"
                  onClick={() => {
                    if (confirm('Delete this table?')) {
                      deleteMutation.mutate(table.id);
                    }
                  }}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
      )}

      {viewMode === 'list' && tables.length === 0 && (
        <Card className="py-12 text-center">
          <QrCode className="mx-auto h-12 w-12 text-muted-foreground" />
          <h3 className="mt-4 font-semibold">No tables yet</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            Add your first table to generate a QR code
          </p>
          <Button className="mt-4" onClick={() => setShowAddDialog(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Add Table
          </Button>
        </Card>
      )}

      {/* Add Table Dialog */}
      <Dialog open={showAddDialog} onOpenChange={setShowAddDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add New Table</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleAddTable} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="tableNumber">Table Number *</Label>
              <Input
                id="tableNumber"
                name="tableNumber"
                placeholder="1, A1, Patio-1..."
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="tableName">Table Name (optional)</Label>
              <Input
                id="tableName"
                name="tableName"
                placeholder="Window seat, VIP booth..."
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="capacity">Capacity</Label>
              <Input
                id="capacity"
                name="capacity"
                type="number"
                min="1"
                max="20"
                defaultValue="4"
              />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setShowAddDialog(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? 'Creating...' : 'Create Table'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Edit Table Dialog */}
      <Dialog open={!!selectedTable} onOpenChange={() => setSelectedTable(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit Table {selectedTable?.tableNumber}</DialogTitle>
          </DialogHeader>
          {selectedTable && (
            <form onSubmit={handleUpdateTable} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="editTableName">Table Name</Label>
                <Input
                  id="editTableName"
                  name="tableName"
                  defaultValue={selectedTable.tableName || ''}
                  placeholder="Window seat, VIP booth..."
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="editCapacity">Capacity</Label>
                <Input
                  id="editCapacity"
                  name="capacity"
                  type="number"
                  min="1"
                  max="20"
                  defaultValue={selectedTable.capacity}
                />
              </div>
              <div className="flex items-center gap-2">
                <Switch
                  id="active"
                  name="active"
                  defaultChecked={selectedTable.active}
                />
                <Label htmlFor="active">Active</Label>
              </div>
              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setSelectedTable(null)}>
                  Cancel
                </Button>
                <Button type="submit" disabled={updateMutation.isPending}>
                  {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
                </Button>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>

      {/* QR Code Dialog */}
      <Dialog open={!!qrTable} onOpenChange={() => setQrTable(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="text-center">
              QR Code - Table {qrTable?.tableNumber}
            </DialogTitle>
          </DialogHeader>
          {qrTable && (
            <div className="space-y-4">
              <div 
                ref={qrRef}
                className="flex flex-col items-center rounded-xl bg-white p-8"
              >
                <QRCodeSVG
                  value={getTableUrl(qrTable)}
                  size={250}
                  level="H"
                  includeMargin={false}
                />
                <div className="mt-4 text-center">
                  <h3 className="text-xl font-bold text-gray-900">
                    Table {qrTable.tableNumber}
                  </h3>
                  {qrTable.tableName && (
                    <p className="text-sm text-gray-600">{qrTable.tableName}</p>
                  )}
                  <p className="mt-2 text-xs text-gray-400">Scan to view menu</p>
                </div>
              </div>

              <div className="rounded-lg bg-muted p-3">
                <p className="mb-1 text-xs font-medium text-muted-foreground">URL</p>
                <p className="break-all text-xs font-mono">{getTableUrl(qrTable)}</p>
              </div>

              <Button className="w-full" onClick={downloadQr}>
                <Download className="mr-2 h-4 w-4" />
                Download PNG
              </Button>

              <p className="text-center text-xs text-muted-foreground">
                Print this QR code and place it on the table. Customers scan to view the menu and place orders.
              </p>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
