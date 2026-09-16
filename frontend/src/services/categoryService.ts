import { request } from './api';
import type { Category } from '../types';

export interface CategoryRequest {
  name: string;
}

export const categoriesApi = {
  list() {
    return request<Category[]>('/categories');
  },

  getById(id: number) {
    return request<Category>(`/categories/${id}`);
  },

  create(data: CategoryRequest) {
    return request<Category>('/categories', {
      method: 'POST',
      auth: true,
      body: data,
    });
  },

  update(id: number, data: CategoryRequest) {
    return request<Category>(`/categories/${id}`, {
      method: 'PUT',
      auth: true,
      body: data,
    });
  },

  remove(id: number) {
    return request<void>(`/categories/${id}`, {
      method: 'DELETE',
      auth: true,
    });
  },
};

export const getCategories = categoriesApi.list;
export const getCategoryById = categoriesApi.getById;
export const createCategory = categoriesApi.create;
export const updateCategory = categoriesApi.update;
export const deleteCategory = categoriesApi.remove;
