import { request } from './api';
import type { Page, Review } from '../types';

export interface ReviewQuery {
  page?: number;
  size?: number;
}

export interface CreateReviewRequest {
  productId: number;
  rating: number;
  comment: string;
}

export const reviewsApi = {
  listByProduct(productId: number, query: ReviewQuery = {}) {
    const params = new URLSearchParams();
    if (query.page !== undefined) params.set('page', String(query.page));
    if (query.size !== undefined) params.set('size', String(query.size));
    const suffix = params.toString() ? `?${params.toString()}` : '';
    return request<Page<Review>>(`/products/${productId}/reviews${suffix}`);
  },

  create(data: CreateReviewRequest) {
    return request<Review>('/reviews', {
      method: 'POST',
      auth: true,
      body: data,
    });
  },

  remove(id: number) {
    return request<void>(`/reviews/${id}`, {
      method: 'DELETE',
      auth: true,
    });
  },
};

export const getProductReviews = reviewsApi.listByProduct;
export const createReview = reviewsApi.create;
export const deleteReview = reviewsApi.remove;
