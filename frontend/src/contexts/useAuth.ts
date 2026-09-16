import { useContext } from 'react';
import { AuthContext } from './AuthContext';
import type { AuthContextValue } from './auth.types';

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth deve ser usado dentro de AuthProvider.');
  }
  return context;
}
