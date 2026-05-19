import { apiClient } from './client';
import type { 
  TableMenuResponse, 
  JoinResponse, 
  OrderResponse, 
  RecordEventRequest,
  MenuSection,
} from '../types';

export interface PublicMenuResponse {
  restaurantName: string;
  slug: string;
  sections: MenuSection[];
}

export const menuApi = {
  getMenu: async (slug: string): Promise<PublicMenuResponse> => {
    const response = await apiClient.get<PublicMenuResponse>(`/api/menu/${slug}`);
    return response.data;
  },

  getTableMenu: async (qrToken: string): Promise<TableMenuResponse> => {
    const response = await apiClient.get<TableMenuResponse>(`/api/table/${qrToken}`);
    return response.data;
  },

  joinTable: async (qrToken: string, sessionCode?: string): Promise<JoinResponse> => {
    const response = await apiClient.post<JoinResponse>(
      `/api/table/${qrToken}/join`,
      { sessionCode }
    );
    return response.data;
  },

  getOrder: async (qrToken: string, sessionId: string): Promise<OrderResponse> => {
    const response = await apiClient.get<OrderResponse>(
      `/api/table/${qrToken}/order`,
      { params: { sessionId } }
    );
    return response.data;
  },

  addItem: async (
    qrToken: string,
    sessionId: string,
    menuItemId: string,
    quantity: number,
    notes?: string,
    guestName?: string,
    selectedModifierIds?: string[]
  ): Promise<OrderResponse> => {
    const response = await apiClient.post<OrderResponse>(
      `/api/table/${qrToken}/order/items`,
      { sessionId, menuItemId, quantity, notes, guestName, selectedModifierIds }
    );
    return response.data;
  },

  updateItemQuantity: async (
    qrToken: string,
    itemId: string,
    sessionId: string,
    quantity: number
  ): Promise<OrderResponse> => {
    const response = await apiClient.put<OrderResponse>(
      `/api/table/${qrToken}/order/items/${itemId}`,
      { sessionId, quantity }
    );
    return response.data;
  },

  removeItem: async (
    qrToken: string,
    itemId: string,
    sessionId: string
  ): Promise<void> => {
    await apiClient.delete(
      `/api/table/${qrToken}/order/items/${itemId}`,
      { params: { sessionId } }
    );
  },

  submitOrder: async (
    qrToken: string,
    sessionId: string,
    notes?: string
  ): Promise<OrderResponse> => {
    const response = await apiClient.post<OrderResponse>(
      `/api/table/${qrToken}/order/submit`,
      { sessionId, notes }
    );
    return response.data;
  },

  requestBill: async (
    qrToken: string,
    sessionId: string
  ): Promise<OrderResponse> => {
    const response = await apiClient.post<OrderResponse>(
      `/api/table/${qrToken}/order/request-bill`,
      { sessionId }
    );
    return response.data;
  },

  recordEvent: async (slug: string, event: RecordEventRequest): Promise<void> => {
    await apiClient.post(`/api/menu/${slug}/events`, event);
  },

  getRecommendations: async (
    tenantId: string,
    cartItemIds: string[],
    menuItemIds: string[]
  ): Promise<string[]> => {
    const response = await apiClient.post<{ recommended_items?: string[] }>(
      `/api/menu/${tenantId}/recommendations`,
      { items_in_cart: cartItemIds, menu_item_ids: menuItemIds }
    );
    return response.data.recommended_items || [];
  },
};
