import client from './client';
import type {
  IncidentResponse,
  PageResponse,
  CreateIncidentRequest,
  UpdateIncidentRequest,
  ResolveIncidentRequest,
  AddCommentRequest,
  CommentResponse,
  HistoryResponse,
  IncidentFilters,
  SearchResult,
} from '@/types';

export const incidentsApi = {
  list(filters: IncidentFilters = {}) {
    const params = new URLSearchParams();
    if (filters.status) params.set('status', filters.status);
    if (filters.priority) params.set('priority', filters.priority);
    if (filters.category) params.set('category', filters.category);
    if (filters.page !== undefined) params.set('page', String(filters.page));
    if (filters.size !== undefined) params.set('size', String(filters.size));
    return client.get<PageResponse<IncidentResponse>>('/incidents', { params });
  },

  get(id: string) {
    return client.get<IncidentResponse>(`/incidents/${id}`);
  },

  create(data: CreateIncidentRequest) {
    return client.post<IncidentResponse>('/incidents', data);
  },

  update(id: string, data: UpdateIncidentRequest) {
    return client.put<IncidentResponse>(`/incidents/${id}`, data);
  },

  resolve(id: string, data: ResolveIncidentRequest) {
    return client.patch<IncidentResponse>(`/incidents/${id}/resolve`, data);
  },

  delete(id: string) {
    return client.delete<void>(`/incidents/${id}`);
  },

  search(q: string, page = 0, size = 20) {
    return client.get<PageResponse<SearchResult>>('/incidents/search', {
      params: { q, page, size },
    });
  },

  getComments(id: string, page = 0, size = 20) {
    return client.get<PageResponse<CommentResponse>>(`/incidents/${id}/comments`, {
      params: { page, size },
    });
  },

  addComment(id: string, data: AddCommentRequest) {
    return client.post<CommentResponse>(`/incidents/${id}/comments`, data);
  },

  getHistory(id: string, page = 0, size = 20) {
    return client.get<PageResponse<HistoryResponse>>(`/incidents/${id}/history`, {
      params: { page, size },
    });
  },
};
