import { request } from './api';
import type { Cart } from '../types';

export interface AddCartItemRequest {
  productId: number;
  quantity: number;
}

export const cartApi = {
  get() {
    return request<Cart>('/cart', { auth: true });
  },

  addItem(data: AddCartItemRequest) {
    return request<Cart>('/cart/items', {
      method: 'POST',
      auth: true,
      body: data,
    });
  },

  updateItem(productId: number, quantity: number) {
    return request<Cart>(
      `/cart/items/${productId}?quantity=${encodeURIComponent(quantity)}`,
      {
        method: 'PUT',
        auth: true,
      },
    );
  },

  clear() {
    return request<void>('/cart', {
      method: 'DELETE',
      auth: true,
    });
  },
};

export const getCart = cartApi.get;
export const addCartItem = cartApi.addItem;
export const updateCartItem = cartApi.updateItem;
export const clearCart = cartApi.clear;
