import { request } from './api';
import type { Page, Product } from '../types';

export interface ProductQuery {
  search?: string;
  categoryId?: number;
  page?: number;
  size?: number;
}

export interface ProductRequest {
  name: string;
  description: string | null;
  price: number;
  stock: number;
  imageUrl: string | null;
  categoryId: number;
}

export const productsApi = {
  list(query: ProductQuery = {}) {
    const params = new URLSearchParams();
    if (query.search) params.set('search', query.search);
    if (query.categoryId !== undefined) params.set('categoryId', String(query.categoryId));
    if (query.page !== undefined) params.set('page', String(query.page));
    if (query.size !== undefined) params.set('size', String(query.size));
    const suffix = params.toString() ? `?${params.toString()}` : '';
    return request<Page<Product>>(`/products${suffix}`);
  },

  getById(id: number) {
    return request<Product>(`/products/${id}`);
  },

  create(data: ProductRequest) {
    return request<Product>('/products', {
      method: 'POST',
      auth: true,
      body: data,
    });
  },

  update(id: number, data: ProductRequest) {
    return request<Product>(`/products/${id}`, {
      method: 'PUT',
      auth: true,
      body: data,
    });
  },

  remove(id: number) {
    return request<void>(`/products/${id}`, {
      method: 'DELETE',
      auth: true,
    });
  },
};

export const getProducts = productsApi.list;
export const getProductById = productsApi.getById;
export const createProduct = productsApi.create;
export const updateProduct = productsApi.update;
export const deleteProduct = productsApi.remove;
