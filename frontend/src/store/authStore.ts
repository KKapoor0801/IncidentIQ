import { create } from 'zustand';
import { authApi } from '@/api/auth';
import { configureAuth } from '@/api/client';
import { UserRole, type JwtPayload } from '@/types';

interface AuthUser {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  isInitializing: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string) => Promise<void>;
  logout: () => void;
  refreshAccessToken: () => Promise<string | null>;
  initializeFromStorage: () => Promise<void>;
  clearError: () => void;
}

const REFRESH_TOKEN_KEY = 'incidentiq_refresh_token';

function decodeJwt(token: string): JwtPayload {
  const base64Url = token.split('.')[1];
  if (!base64Url) throw new Error('Invalid JWT');
  const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
  const jsonPayload = decodeURIComponent(
    atob(base64)
      .split('')
      .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
      .join('')
  );
  return JSON.parse(jsonPayload) as JwtPayload;
}

function userFromToken(token: string): AuthUser {
  const payload = decodeJwt(token);
  return {
    id: payload.sub,
    email: payload.email,
    fullName: payload.fullName,
    role: payload.role,
  };
}

function persistRefreshToken(token: string | null) {
  if (token) {
    localStorage.setItem(REFRESH_TOKEN_KEY, token);
  } else {
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }
}

function getStoredRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export const useAuthStore = create<AuthState>((set, get) => {
  const store: AuthState = {
    accessToken: null,
    refreshToken: null,
    user: null,
    isAuthenticated: false,
    isLoading: false,
    isInitializing: true,
    error: null,

    async login(email: string, password: string) {
      set({ isLoading: true, error: null });
      try {
        const { data } = await authApi.login({ email, password });
        const user = userFromToken(data.accessToken);
        persistRefreshToken(data.refreshToken);
        set({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          user,
          isAuthenticated: true,
          isLoading: false,
        });
      } catch (err) {
        const message = getErrorMessage(err);
        set({ isLoading: false, error: message });
        throw err;
      }
    },

    async register(email: string, password: string, fullName: string) {
      set({ isLoading: true, error: null });
      try {
        const { data } = await authApi.register({ email, password, fullName });
        const user = userFromToken(data.accessToken);
        persistRefreshToken(data.refreshToken);
        set({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          user,
          isAuthenticated: true,
          isLoading: false,
        });
      } catch (err) {
        const message = getErrorMessage(err);
        set({ isLoading: false, error: message });
        throw err;
      }
    },

    logout() {
      const token = get().accessToken;
      if (token) {
        authApi.logout().catch(() => {});
      }
      persistRefreshToken(null);
      set({
        accessToken: null,
        refreshToken: null,
        user: null,
        isAuthenticated: false,
        error: null,
      });
    },

    async refreshAccessToken() {
      const currentRefreshToken = get().refreshToken ?? getStoredRefreshToken();
      if (!currentRefreshToken) {
        get().logout();
        return null;
      }
      try {
        const { data } = await authApi.refresh({ refreshToken: currentRefreshToken });
        const user = userFromToken(data.accessToken);
        persistRefreshToken(data.refreshToken);
        set({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          user,
          isAuthenticated: true,
        });
        return data.accessToken;
      } catch {
        get().logout();
        return null;
      }
    },

    async initializeFromStorage() {
      const storedRefreshToken = getStoredRefreshToken();
      if (!storedRefreshToken) {
        set({ isInitializing: false });
        return;
      }
      try {
        const { data } = await authApi.refresh({ refreshToken: storedRefreshToken });
        const user = userFromToken(data.accessToken);
        persistRefreshToken(data.refreshToken);
        set({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          user,
          isAuthenticated: true,
          isInitializing: false,
        });
      } catch {
        persistRefreshToken(null);
        set({ isInitializing: false });
      }
    },

    clearError() {
      set({ error: null });
    },
  };

  return store;
});

function getErrorMessage(err: unknown): string {
  if (
    typeof err === 'object' &&
    err !== null &&
    'response' in err
  ) {
    const response = (err as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) {
      return response.data.message;
    }
  }
  return 'An unexpected error occurred';
}

// Configure the axios client with auth callbacks
configureAuth({
  getAccessToken: () => useAuthStore.getState().accessToken,
  onRefreshToken: () => useAuthStore.getState().refreshAccessToken(),
  onLogout: () => useAuthStore.getState().logout(),
});
