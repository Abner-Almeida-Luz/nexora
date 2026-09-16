import { request } from './api';
import type { Order, OrderStatus } from '../types';

export const ordersApi = {
  create() {
    return request<Order>('/orders', {
      method: 'POST',
      auth: true,
    });
  },

  list() {
    return request<Order[]>('/orders', { auth: true });
  },

  getById(id: number) {
    return request<Order>(`/orders/${id}`, { auth: true });
  },

  listAll() {
    return request<Order[]>('/orders/admin/all', { auth: true });
  },

  updateStatus(id: number, status: OrderStatus) {
    return request<Order>(`/orders/${id}/status`, {
      method: 'PATCH',
      auth: true,
      body: { status },
    });
  },

  cancel(id: number) {
    // TODO: depende de PATCH /api/orders/{id}/cancel no backend.
    return request<Order>(`/orders/${id}/cancel`, {
      method: 'PATCH',
      auth: true,
    });
  },
};

export const createOrder = ordersApi.create;
export const getOrders = ordersApi.list;
export const getOrderById = ordersApi.getById;
export const getAllOrders = ordersApi.listAll;
export const updateOrderStatus = ordersApi.updateStatus;
export const cancelOrder = ordersApi.cancel;
