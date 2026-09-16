import { request } from './api';
import type { AuthResponse } from '../types';

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export function register(data: RegisterRequest): Promise<AuthResponse & { refreshToken?: string }> {
  return request<AuthResponse & { refreshToken?: string }>('/auth/register', {
    method: 'POST',
    body: data,
  });
}

export function login(data: LoginRequest): Promise<AuthResponse & { refreshToken?: string }> {
  return request<AuthResponse & { refreshToken?: string }>('/auth/login', {
    method: 'POST',
    body: data,
  });
}
