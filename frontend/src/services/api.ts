import type { RefreshResponse } from '../types';

export interface ApiErrorBody {
  type: string;
  title: string;
  status: number;
  detail: string;
  timestamp: string;
  errors?: Record<string, string>;
}

export class ApiError extends Error {
  readonly type: string;
  readonly title: string;
  readonly status: number;
  readonly detail: string;
  readonly timestamp: string;
  readonly errors?: Record<string, string>;

  constructor(body: ApiErrorBody) {
    super(body.detail);
    this.name = 'ApiError';
    this.type = body.type;
    this.title = body.title;
    this.status = body.status;
    this.detail = body.detail;
    this.timestamp = body.timestamp;
    this.errors = body.errors;
  }
}

interface RequestOptions extends Omit<RequestInit, 'body'> {
  auth?: boolean;
  body?: unknown;
}

const API_PREFIX = `${import.meta.env.VITE_API_BASE_URL || ''}/api`;
const TOKEN_KEY = 'token';
const REFRESH_TOKEN_KEY = 'refreshToken';

let onUnauthorized: (() => void) | null = null;
let refreshPromise: Promise<string | null> | null = null;

export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { auth = false, body, headers, ...init } = options;

  const execute = async (tokenOverride?: string | null): Promise<Response> => {
    const requestHeaders = new Headers(headers);
    requestHeaders.set('Accept', 'application/json, application/problem+json');

    if (body !== undefined) {
      requestHeaders.set('Content-Type', 'application/json');
    }

    const token = tokenOverride ?? (auth ? localStorage.getItem(TOKEN_KEY) : null);
    if (auth && token) {
      requestHeaders.set('Authorization', `Bearer ${token}`);
    }

    return fetch(`${API_PREFIX}${path}`, {
      ...init,
      headers: requestHeaders,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  };

  let response = await execute();

  if (auth && response.status === 401) {
    const token = await refreshAccessToken();

    if (token) {
      response = await execute(token);
    }

    if (response.status === 401) {
      handleUnauthorized();
    }
  }

  if (!response.ok) {
    await throwApiError(response);
  }

  return parseResponse<T>(response);
}

async function refreshAccessToken(): Promise<string | null> {
  if (refreshPromise) {
    return refreshPromise;
  }

  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);

  if (!refreshToken) {
    return null;
  }

  refreshPromise = (async () => {
    try {
      const response = await fetch(`${API_PREFIX}/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json, application/problem+json',
        },
        body: JSON.stringify({ refreshToken }),
      });

      if (!response.ok) {
        return null;
      }

      const data = (await response.json()) as RefreshResponse;

      localStorage.setItem(TOKEN_KEY, data.token);
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);

      return data.token;
    } catch {
      return null;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

function handleUnauthorized() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem('user');

  onUnauthorized?.();

  if (window.location.pathname !== '/login') {
    window.location.assign('/login');
  }
}

async function parseResponse<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get('content-type');

  if (!contentType?.includes('json')) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

async function throwApiError(response: Response): Promise<never> {
  const contentType = response.headers.get('content-type');

  if (contentType?.includes('json')) {
    const raw = (await response.json()) as Partial<ApiErrorBody>;

    throw new ApiError({
      type: raw.type ?? 'about:blank',
      title: raw.title ?? response.statusText ?? 'HTTP Error',
      status: raw.status ?? response.status,
      detail: raw.detail ?? 'Ocorreu um erro ao processar a requisição.',
      timestamp: raw.timestamp ?? new Date().toISOString(),
      errors: raw.errors,
    });
  }

  const text = await response.text();

  throw new ApiError({
    type: 'about:blank',
    title: response.statusText || 'HTTP Error',
    status: response.status,
    detail: text || 'Ocorreu um erro ao processar a requisição.',
    timestamp: new Date().toISOString(),
  });
}
