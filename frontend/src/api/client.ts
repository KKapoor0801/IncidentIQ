import axios, { type InternalAxiosRequestConfig } from 'axios';

const client = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

let getAccessToken: (() => string | null) | null = null;
let onRefreshToken: (() => Promise<string | null>) | null = null;
let onLogout: (() => void) | null = null;

export function configureAuth(config: {
  getAccessToken: () => string | null;
  onRefreshToken: () => Promise<string | null>;
  onLogout: () => void;
}) {
  getAccessToken = config.getAccessToken;
  onRefreshToken = config.onRefreshToken;
  onLogout = config.onLogout;
}

client.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken?.();
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string | null) => void;
  reject: (error: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null = null) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
}

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve: () => {
            originalRequest.headers.Authorization = `Bearer ${getAccessToken?.()}`;
            resolve(client(originalRequest));
          }, reject });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const newToken = await onRefreshToken?.();
        if (newToken) {
          processQueue(null, newToken);
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return client(originalRequest);
        } else {
          processQueue(new Error('Refresh failed'));
          onLogout?.();
          return Promise.reject(error);
        }
      } catch (refreshError) {
        processQueue(refreshError);
        onLogout?.();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default client;
