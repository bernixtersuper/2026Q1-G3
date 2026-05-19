import { createContext, useContext, useState, useCallback, useEffect, useRef, type ReactNode } from 'react';
import { useMutation } from '@tanstack/react-query';
import { menuApi } from '@/shared/api/menuApi';
import { useToast } from '@/components/Toast';
import type { OrderResponse } from '@/shared/types';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

interface OrderContextValue {
  order: OrderResponse | null;
  sessionId: string | null;
  sessionCode: string | null;
  tableNumber: string | null;
  isLoading: boolean;
  setSession: (sessionId: string, sessionCode: string, tableNumber: string) => void;
  setOrder: (order: OrderResponse | null) => void;
  addItem: (qrToken: string, menuItemId: string, name: string, price: string, quantity: number, notes?: string, guestName?: string, selectedModifierIds?: string[]) => Promise<void>;
  updateQuantity: (qrToken: string, itemId: string, quantity: number) => Promise<void>;
  removeItem: (qrToken: string, itemId: string) => Promise<void>;
  submitOrder: (qrToken: string, notes?: string) => Promise<void>;
  requestBill: (qrToken: string) => Promise<void>;
  getItemCount: () => number;
  getTotal: () => string;
  subscribeToUpdates: (qrToken: string) => void;
}

const OrderContext = createContext<OrderContextValue | null>(null);

export function useOrder() {
  const ctx = useContext(OrderContext);
  if (!ctx) throw new Error('useOrder must be used within OrderProvider');
  return ctx;
}

export function OrderProvider({ children }: { children: ReactNode }) {
  const { showToast } = useToast();
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [sessionCode, setSessionCode] = useState<string | null>(null);
  const [tableNumber, setTableNumber] = useState<string | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);
  const currentQrTokenRef = useRef<string | null>(null);

  const subscribeToUpdates = useCallback((qrToken: string) => {
    if (!order?.id || order.status === 'DRAFT') return;
    
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }
    
    currentQrTokenRef.current = qrToken;
    const url = `${API_URL}/api/table/${qrToken}/order/${order.id}/events`;
    const eventSource = new EventSource(url);
    eventSourceRef.current = eventSource;
    
    eventSource.addEventListener('order-update', (event) => {
      try {
        const data = JSON.parse(event.data);
        setOrder(prev => prev ? { ...prev, status: data.status } : prev);
        
        const statusMessages: Record<string, string> = {
          CONFIRMED: 'Your order has been confirmed!',
          PREPARING: 'Your order is being prepared',
          READY: 'Your order is ready!',
          DELIVERED: 'Your order has been delivered',
          CANCELLED: 'Your order has been cancelled',
        };
        const message = statusMessages[data.status];
        if (message) {
          showToast(message, data.status === 'CANCELLED' ? 'error' : 'success');
        }
      } catch (e) {
        console.error('Failed to parse SSE event:', e);
      }
    });
    
    eventSource.addEventListener('connected', () => {
      console.log('Connected to order updates');
    });
    
    eventSource.onerror = () => {
      console.log('SSE connection error, will retry...');
    };
  }, [order?.id, order?.status]);

  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, []);

  useEffect(() => {
    if (order?.id && order.status !== 'DRAFT' && currentQrTokenRef.current) {
      subscribeToUpdates(currentQrTokenRef.current);
    }
  }, [order?.id, order?.status, subscribeToUpdates]);

  const addItemMutation = useMutation({
    mutationFn: async ({ qrToken, menuItemId, quantity, notes, guestName, selectedModifierIds }: {
      qrToken: string;
      menuItemId: string;
      quantity: number;
      notes?: string;
      guestName?: string;
      selectedModifierIds?: string[];
    }) => {
      if (!sessionId) throw new Error('No session');
      return menuApi.addItem(qrToken, sessionId, menuItemId, quantity, notes, guestName, selectedModifierIds);
    },
    onSuccess: (data) => {
      setOrder(data);
      showToast('Added to order', 'success');
    },
    onError: () => showToast('Failed to add item', 'error'),
  });

  const updateQuantityMutation = useMutation({
    mutationFn: async ({ qrToken, itemId, quantity }: {
      qrToken: string;
      itemId: string;
      quantity: number;
    }) => {
      if (!sessionId) throw new Error('No session');
      return menuApi.updateItemQuantity(qrToken, itemId, sessionId, quantity);
    },
    onSuccess: (data) => setOrder(data),
    onError: () => showToast('Failed to update quantity', 'error'),
  });

  const removeItemMutation = useMutation({
    mutationFn: async ({ qrToken, itemId }: { qrToken: string; itemId: string }) => {
      if (!sessionId) throw new Error('No session');
      await menuApi.removeItem(qrToken, itemId, sessionId);
      const updatedOrder = await menuApi.getOrder(qrToken, sessionId);
      return updatedOrder;
    },
    onSuccess: (data) => {
      setOrder(data);
      showToast('Item removed', 'info');
    },
    onError: () => showToast('Failed to remove item', 'error'),
  });

  const submitOrderMutation = useMutation({
    mutationFn: async ({ qrToken, notes }: { qrToken: string; notes?: string }) => {
      if (!sessionId) throw new Error('No session');
      currentQrTokenRef.current = qrToken;
      return menuApi.submitOrder(qrToken, sessionId, notes);
    },
    onSuccess: (data) => {
      setOrder(data);
      showToast('Order submitted! Waiting for confirmation', 'success');
      if (data.id && currentQrTokenRef.current) {
        setTimeout(() => subscribeToUpdates(currentQrTokenRef.current!), 100);
      }
    },
    onError: () => showToast('Failed to submit order', 'error'),
  });

  const requestBillMutation = useMutation({
    mutationFn: async ({ qrToken }: { qrToken: string }) => {
      if (!sessionId) throw new Error('No session');
      return menuApi.requestBill(qrToken, sessionId);
    },
    onSuccess: (data) => {
      setOrder(data);
      showToast('Bill requested - staff notified', 'success');
    },
    onError: () => showToast('Failed to request bill', 'error'),
  });

  const setSession = useCallback((sid: string, code: string, table: string) => {
    setSessionId(sid);
    setSessionCode(code);
    setTableNumber(table);
  }, []);

  const addItem = useCallback(async (
    qrToken: string,
    menuItemId: string,
    _name: string,
    _price: string,
    quantity: number,
    notes?: string,
    guestName?: string,
    selectedModifierIds?: string[]
  ) => {
    await addItemMutation.mutateAsync({ qrToken, menuItemId, quantity, notes, guestName, selectedModifierIds });
  }, [addItemMutation]);

  const updateQuantity = useCallback(async (qrToken: string, itemId: string, quantity: number) => {
    await updateQuantityMutation.mutateAsync({ qrToken, itemId, quantity });
  }, [updateQuantityMutation]);

  const removeItem = useCallback(async (qrToken: string, itemId: string) => {
    await removeItemMutation.mutateAsync({ qrToken, itemId });
  }, [removeItemMutation]);

  const submitOrder = useCallback(async (qrToken: string, notes?: string) => {
    await submitOrderMutation.mutateAsync({ qrToken, notes });
  }, [submitOrderMutation]);

  const requestBill = useCallback(async (qrToken: string) => {
    await requestBillMutation.mutateAsync({ qrToken });
  }, [requestBillMutation]);

  const getItemCount = useCallback(() => {
    return order?.items.reduce((sum, item) => sum + item.quantity, 0) || 0;
  }, [order]);

  const getTotal = useCallback(() => {
    return order?.subtotal || '0.00';
  }, [order]);

  const isLoading = addItemMutation.isPending || 
    updateQuantityMutation.isPending || 
    removeItemMutation.isPending || 
    submitOrderMutation.isPending ||
    requestBillMutation.isPending;

  return (
    <OrderContext.Provider value={{
      order,
      sessionId,
      sessionCode,
      tableNumber,
      isLoading,
      setSession,
      setOrder,
      addItem,
      updateQuantity,
      removeItem,
      submitOrder,
      requestBill,
      getItemCount,
      getTotal,
      subscribeToUpdates,
    }}>
      {children}
    </OrderContext.Provider>
  );
}
