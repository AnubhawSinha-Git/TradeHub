import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';

import { api } from './api';
import { getToken, setToken } from './storage';
import type { LoginResponse, UserProfile } from './types';

interface AuthState {
  user: UserProfile | null;
  token: string | null;
  loading: boolean;

  login: (email: string, password: string) => Promise<void>;
  register: (fullName: string, email: string, password: string) => Promise<void>;
  logout: () => void;

  isAdmin: boolean;
  isWarehouse: boolean;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [token, setTokenState] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      const stored = await getToken();
      if (stored) {
        try {
          const profile = await api<UserProfile>('/api/auth/me', { token: stored });
          setUser(profile);
          setTokenState(stored);
        } catch {
          await setToken('');
          setTokenState(null);
          setUser(null);
        }
      }
      setLoading(false);
    })();
  }, []);

  async function fetchAndSet(newToken: string): Promise<void> {
    const profile = await api<UserProfile>('/api/auth/me', { token: newToken });
    await setToken(newToken);
    setTokenState(newToken);
    setUser(profile);
  }

  async function login(email: string, password: string): Promise<void> {
    const res = await api<LoginResponse>('/api/auth/login', {
      method: 'POST',
      body: { email, password },
    });
    await fetchAndSet(res.accessToken);
  }

  async function register(fullName: string, email: string, password: string): Promise<void> {
    await api('/api/auth/register', {
      method: 'POST',
      body: { fullName, email, password },
    });
    await login(email, password);
  }

  async function logout(): Promise<void> {
    await setToken('');
    setTokenState(null);
    setUser(null);
  }

  const isAdmin = user?.roles.includes('ADMIN') ?? false;
  const isWarehouse = user?.roles.includes('WAREHOUSE') ?? false;

  const state = useMemo<AuthState>(
    () => ({
      user,
      token,
      loading,
      login,
      register,
      logout,
      isAdmin,
      isWarehouse,
    }),
    [user, token, loading, isAdmin, isWarehouse],
  );

  return <AuthContext.Provider value={state}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return ctx;
}