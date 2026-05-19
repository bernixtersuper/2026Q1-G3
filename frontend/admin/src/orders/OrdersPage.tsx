import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { orderApi, type Order } from '../shared/api/adminApi';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Clock, ChefHat, Check, XCircle, RefreshCw, Receipt } from 'lucide-react';
import { toast } from '../hooks/use-toast';

const statusConfig: Record<string, { label: string; color: string; icon: typeof Clock }> = {
  DRAFT: { label: 'Draft', color: 'bg-gray-500', icon: Clock },
  SUBMITTED: { label: 'New', color: 'bg-yellow-500', icon: Clock },
  CONFIRMED: { label: 'Confirmed', color: 'bg-blue-500', icon: Check },
  PREPARING: { label: 'Preparing', color: 'bg-purple-500', icon: ChefHat },
  READY: { label: 'Ready', color: 'bg-green-500', icon: Check },
  DELIVERED: { label: 'Delivered', color: 'bg-gray-400', icon: Check },
  BILL_REQUESTED: { label: 'Bill Requested', color: 'bg-orange-500', icon: Receipt },
  CANCELLED: { label: 'Cancelled', color: 'bg-red-500', icon: XCircle },
};

const formatCurrency = (value: string) => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(parseFloat(value));
};

const formatTime = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

export function OrdersPage() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<'active' | 'all'>('active');

  const { data: orders = [], isLoading, refetch } = useQuery({
    queryKey: ['orders', filter],
    queryFn: () => filter === 'active' ? orderApi.listActive() : orderApi.listAll(),
    refetchInterval: 10000,
  });

  const confirmMutation = useMutation({
    mutationFn: orderApi.confirm,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      toast({ title: 'Order Confirmed', description: `Order #${data.orderNumber} has been confirmed`, variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to confirm order', variant: 'destructive' }),
  });

  const preparingMutation = useMutation({
    mutationFn: orderApi.markPreparing,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      toast({ title: 'Preparing', description: `Order #${data.orderNumber} is now being prepared`, variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to update order', variant: 'destructive' }),
  });

  const readyMutation = useMutation({
    mutationFn: orderApi.markReady,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      toast({ title: 'Ready for Pickup', description: `Order #${data.orderNumber} is ready`, variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to update order', variant: 'destructive' }),
  });

  const deliveredMutation = useMutation({
    mutationFn: orderApi.markDelivered,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      toast({ title: 'Delivered', description: `Order #${data.orderNumber} has been delivered`, variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to update order', variant: 'destructive' }),
  });

  const cancelMutation = useMutation({
    mutationFn: orderApi.cancel,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      toast({ title: 'Order Cancelled', description: `Order #${data.orderNumber} has been cancelled`, variant: 'warning' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to cancel order', variant: 'destructive' }),
  });

  const getNextAction = (order: Order) => {
    switch (order.status) {
      case 'SUBMITTED':
        return { label: 'Confirm', action: () => confirmMutation.mutate(order.id), variant: 'default' as const };
      case 'CONFIRMED':
        return { label: 'Start Preparing', action: () => preparingMutation.mutate(order.id), variant: 'default' as const };
      case 'PREPARING':
        return { label: 'Mark Ready', action: () => readyMutation.mutate(order.id), variant: 'default' as const };
      case 'READY':
        return { label: 'Delivered', action: () => deliveredMutation.mutate(order.id), variant: 'default' as const };
      default:
        return null;
    }
  };

  const groupedOrders = orders.reduce((acc, order) => {
    const status = order.status;
    if (!acc[status]) acc[status] = [];
    acc[status].push(order);
    return acc;
  }, {} as Record<string, Order[]>);

  const columnOrder = ['SUBMITTED', 'CONFIRMED', 'PREPARING', 'READY', 'BILL_REQUESTED'];

  if (isLoading) {
    return <div className="flex items-center justify-center py-12">Loading...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Orders Dashboard</h1>
          <p className="text-sm text-muted-foreground">
            Manage incoming orders in real-time
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant={filter === 'active' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setFilter('active')}
          >
            Active
          </Button>
          <Button
            variant={filter === 'all' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setFilter('all')}
          >
            All
          </Button>
          <Button variant="outline" size="icon" onClick={() => refetch()}>
            <RefreshCw className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {filter === 'active' && orders.length > 0 && (
        <div className="grid gap-4 lg:grid-cols-5">
          {columnOrder.map((status) => (
            <div key={status} className="space-y-3">
              <div className="flex items-center gap-2">
                <Badge className={statusConfig[status]?.color}>
                  {statusConfig[status]?.label}
                </Badge>
                <span className="text-sm text-muted-foreground">
                  ({groupedOrders[status]?.length || 0})
                </span>
              </div>
              <div className="space-y-3">
                {(groupedOrders[status] || []).map((order) => (
                  <OrderCard
                    key={order.id}
                    order={order}
                    nextAction={getNextAction(order)}
                    onCancel={() => {
                      if (confirm('Cancel this order?')) {
                        cancelMutation.mutate(order.id);
                      }
                    }}
                  />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {filter === 'all' && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {orders.map((order) => (
            <OrderCard
              key={order.id}
              order={order}
              nextAction={getNextAction(order)}
              onCancel={() => {
                if (confirm('Cancel this order?')) {
                  cancelMutation.mutate(order.id);
                }
              }}
            />
          ))}
        </div>
      )}

      {orders.length === 0 && (
        <Card className="py-12 text-center">
          <ChefHat className="mx-auto h-12 w-12 text-muted-foreground" />
          <h3 className="mt-4 font-semibold">No orders yet</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            Orders will appear here when customers submit them
          </p>
        </Card>
      )}
    </div>
  );
}

interface OrderCardProps {
  order: Order;
  nextAction: { label: string; action: () => void; variant: 'default' | 'outline' } | null;
  onCancel: () => void;
}

function OrderCard({ order, nextAction, onCancel }: OrderCardProps) {
  const config = statusConfig[order.status] || statusConfig.DRAFT;
  const Icon = config.icon;

  return (
    <Card className="overflow-hidden">
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Badge className={config.color}>
              <Icon className="mr-1 h-3 w-3" />
              {config.label}
            </Badge>
            {order.orderNumber > 0 && (
              <span className="text-sm font-semibold">#{order.orderNumber}</span>
            )}
          </div>
          <span className="text-lg font-bold">{formatCurrency(order.subtotal)}</span>
        </div>
        <div className="flex items-center gap-4 text-sm text-muted-foreground">
          <span className="font-medium">Table {order.tableNumber}</span>
          {order.submittedAt && (
            <span>{formatTime(order.submittedAt)}</span>
          )}
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="space-y-2">
          {order.items.map((item) => (
            <div key={item.id} className="text-sm">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <span className="font-medium">{item.quantity}x</span>{' '}
                  <span>{item.name}</span>
                  {item.addedBy && (
                    <span className="ml-1 text-xs text-muted-foreground">
                      ({item.addedBy})
                    </span>
                  )}
                </div>
                <span className="text-muted-foreground">
                  {formatCurrency(item.subtotal)}
                </span>
              </div>
              {item.modifiers && item.modifiers.length > 0 && (
                <div className="ml-4 mt-0.5 space-y-0.5">
                  {item.modifiers.map((mod) => (
                    <p key={mod.id} className="text-xs text-muted-foreground">
                      + {mod.name}
                      {parseFloat(mod.priceAdjustment) !== 0 && (
                        <span className="ml-1 text-primary">
                          ({parseFloat(mod.priceAdjustment) > 0 ? '+' : ''}{formatCurrency(mod.priceAdjustment)})
                        </span>
                      )}
                    </p>
                  ))}
                </div>
              )}
              {item.notes && (
                <p className="ml-4 text-xs italic text-muted-foreground">
                  "{item.notes}"
                </p>
              )}
            </div>
          ))}
        </div>

        {order.notes && (
          <div className="rounded-lg bg-muted/50 p-2 text-sm">
            <span className="font-medium">Note:</span> {order.notes}
          </div>
        )}

        {(nextAction || order.status !== 'DELIVERED' && order.status !== 'CANCELLED') && (
          <div className="flex gap-2 border-t pt-3">
            {nextAction && (
              <Button
                size="sm"
                variant={nextAction.variant}
                className="flex-1"
                onClick={nextAction.action}
              >
                {nextAction.label}
              </Button>
            )}
            {order.status !== 'DELIVERED' && order.status !== 'CANCELLED' && (
              <Button
                size="sm"
                variant="ghost"
                className="text-destructive hover:text-destructive"
                onClick={onCancel}
              >
                Cancel
              </Button>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
