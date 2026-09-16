import { describe, expect, it } from 'vitest';
import { ApiError } from './api';

describe('ApiError', () => {
  it('preserva ProblemDetail e erros de campo', () => {
    const error = new ApiError({
      type: 'about:blank',
      title: 'Validation failed',
      status: 400,
      detail: 'Validation failed',
      timestamp: '2026-01-01T00:00:00Z',
      errors: { email: 'E-mail inválido.' },
    });

    expect(error.status).toBe(400);
    expect(error.detail).toBe('Validation failed');
    expect(error.errors?.email).toBe('E-mail inválido.');
  });
});
