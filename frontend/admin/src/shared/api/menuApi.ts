import { apiClient } from './client';
import type { MenuResponse, CreateSectionRequest, CreateItemRequest, MenuSection, MenuItem, Modifier, CreateModifierRequest, UpdateModifierRequest } from '../types';

export const menuApi = {
  getMenu: async (): Promise<MenuResponse> => {
    const response = await apiClient.get<MenuResponse>('/api/admin/menu');
    return response.data;
  },

  updateRestaurant: async (data: { name: string; slug: string }): Promise<void> => {
    await apiClient.put('/api/admin/menu/restaurant', data);
  },

  createSection: async (data: CreateSectionRequest): Promise<MenuSection> => {
    const response = await apiClient.post<MenuSection>('/api/admin/menu/sections', data);
    return response.data;
  },

  updateSection: async (id: string, data: CreateSectionRequest): Promise<void> => {
    await apiClient.put(`/api/admin/menu/sections/${id}`, data);
  },

  deleteSection: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/admin/menu/sections/${id}`);
  },

  createItem: async (data: CreateItemRequest): Promise<MenuItem> => {
    const response = await apiClient.post<MenuItem>('/api/admin/menu/items', data);
    return response.data;
  },

  updateItem: async (id: string, data: CreateItemRequest): Promise<void> => {
    await apiClient.put(`/api/admin/menu/items/${id}`, data);
  },

  deleteItem: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/admin/menu/items/${id}`);
  },

  toggleAvailability: async (id: string, available: boolean): Promise<void> => {
    await apiClient.patch(`/api/admin/menu/items/${id}/availability`, { available });
  },

  uploadImage: async (file: File): Promise<string> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await apiClient.post<{ url: string }>('/api/admin/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data.url;
  },

  getModifiers: async (itemId: string): Promise<Modifier[]> => {
    const response = await apiClient.get<Modifier[]>(`/api/admin/modifiers/item/${itemId}`);
    return response.data;
  },

  createModifier: async (data: CreateModifierRequest): Promise<Modifier> => {
    const response = await apiClient.post<Modifier>('/api/admin/modifiers', data);
    return response.data;
  },

  updateModifier: async (id: string, data: UpdateModifierRequest): Promise<void> => {
    await apiClient.put(`/api/admin/modifiers/${id}`, data);
  },

  deleteModifier: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/admin/modifiers/${id}`);
  },
};
