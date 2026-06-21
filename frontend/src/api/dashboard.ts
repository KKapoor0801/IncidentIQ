import client from './client';
import type { DashboardSummary } from '@/types';

export const dashboardApi = {
  getSummary() {
    return client.get<DashboardSummary>('/dashboard/summary');
  },
};
