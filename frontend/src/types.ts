export enum IncidentStatus {
  OPEN = 'OPEN',
  IN_PROGRESS = 'IN_PROGRESS',
  RESOLVED = 'RESOLVED',
  CLOSED = 'CLOSED',
}

export enum IncidentPriority {
  P1 = 'P1',
  P2 = 'P2',
  P3 = 'P3',
  P4 = 'P4',
}

export enum IncidentCategory {
  PAYMENTS = 'PAYMENTS',
  AUTH = 'AUTH',
  INFRA = 'INFRA',
  DATABASE = 'DATABASE',
  NETWORK = 'NETWORK',
  UNKNOWN = 'UNKNOWN',
}

export enum UserRole {
  ADMIN = 'ADMIN',
  ENGINEER = 'ENGINEER',
  VIEWER = 'VIEWER',
}

export interface UserSummary {
  id: string;
  fullName: string;
  email: string;
}

export interface IncidentResponse {
  id: string;
  title: string;
  description: string;
  status: IncidentStatus;
  priority: IncidentPriority;
  category: IncidentCategory | null;
  aiResolutionSuggestion: string | null;
  aiConfidenceScore: number | null;
  aiProcessed: boolean;
  reporter: UserSummary;
  assignee: UserSummary | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DashboardSummary {
  openCount: number;
  inProgressCount: number;
  resolvedCount: number;
  p1Count: number;
}

export interface CommentResponse {
  id: string;
  body: string;
  author: UserSummary;
  createdAt: string;
}

export interface HistoryResponse {
  id: string;
  fieldChanged: string;
  oldValue: string | null;
  newValue: string | null;
  changedBy: UserSummary;
  changedAt: string;
}

export interface JwtPayload {
  sub: string;
  email: string;
  fullName: string;
  role: UserRole;
  exp: number;
  iat: number;
}

export interface CreateIncidentRequest {
  title: string;
  description: string;
}

export interface UpdateIncidentRequest {
  title?: string;
  description?: string;
  status?: IncidentStatus;
  assigneeId?: string;
}

export interface ResolveIncidentRequest {
  resolutionNotes: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

export interface AddCommentRequest {
  body: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface IncidentFilters {
  status?: IncidentStatus;
  priority?: IncidentPriority;
  category?: IncidentCategory;
  page?: number;
  size?: number;
}

export interface SearchResult {
  [key: string]: unknown;
}
