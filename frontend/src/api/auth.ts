import client from './client';
import type { AuthResponse, LoginRequest, RegisterRequest, RefreshRequest } from '@/types';

export const authApi = {
  login(data: LoginRequest) {
    return client.post<AuthResponse>('/auth/login', data);
  },

  register(data: RegisterRequest) {
    return client.post<AuthResponse>('/auth/register', data);
  },

  refresh(data: RefreshRequest) {
    return client.post<AuthResponse>('/auth/refresh', data);
  },

  logout() {
    return client.post<void>('/auth/logout');
  },
};
