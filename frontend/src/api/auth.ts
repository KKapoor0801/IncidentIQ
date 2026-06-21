import axios from 'axios';
import client from './client';
import type { AuthResponse, LoginRequest, RegisterRequest, RefreshRequest } from '@/types';

const publicClient = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

export const authApi = {
  login(data: LoginRequest) {
    return publicClient.post<AuthResponse>('/auth/login', data);
  },

  register(data: RegisterRequest) {
    return publicClient.post<AuthResponse>('/auth/register', data);
  },

  refresh(data: RefreshRequest) {
    return publicClient.post<AuthResponse>('/auth/refresh', data);
  },

  logout() {
    return client.post<void>('/auth/logout');
  },
};
