import { useState, useEffect, useRef, type FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import Markdown from 'react-markdown';
import { incidentsApi } from '@/api/incidents';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/components/ui/Toast';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { Textarea } from '@/components/ui/Textarea';
import { Dialog } from '@/components/ui/Dialog';
import { Pagination } from '@/components/ui/Pagination';
import { UserRole, IncidentStatus, type CommentResponse, type HistoryResponse } from '@/types';

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

const AI_POLL_TIMEOUT_MS = 120_000;

function extractSuggestionText(raw: string): string {
  try {
    const parsed: unknown = JSON.parse(raw);
    if (typeof parsed === 'object' && parsed !== null) {
      const values = Object.values(parsed as Record<string, unknown>);
      const text = values.find((v) => typeof v === 'string') as string | undefined;
      if (text) return text;
    }
  } catch {
    // not JSON — return as-is
  }
  return raw;
}

function AiSection({ incidentId, aiProcessed, category, priority, confidenceScore, suggestion }: {
  incidentId: string;
  aiProcessed: boolean;
  category: string | null;
  priority: string | null;
  confidenceScore: number | null;
  suggestion: string | null;
}) {
  const [timedOut, setTimedOut] = useState(false);
  const startRef = useRef(Date.now());

  useEffect(() => {
    if (aiProcessed || timedOut) return;
    const timer = setTimeout(() => setTimedOut(true), AI_POLL_TIMEOUT_MS);
    return () => clearTimeout(timer);
  }, [aiProcessed, timedOut]);

  const shouldPoll = !aiProcessed && !timedOut;

  useQuery({
    queryKey: ['incident', incidentId],
    queryFn: () => incidentsApi.get(incidentId).then((r) => r.data),
    refetchInterval: shouldPoll ? 3000 : false,
    enabled: shouldPoll,
  });

  if (!aiProcessed && timedOut) {
    const elapsed = Math.round((Date.now() - startRef.current) / 1000);
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-6 dark:border-amber-800 dark:bg-amber-900/20">
        <h3 className="mb-3 text-sm font-semibold text-gray-900 dark:text-gray-100">
          AI Analysis
        </h3>
        <div className="flex items-start gap-3">
          <svg className="mt-0.5 h-5 w-5 shrink-0 text-amber-500" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9 3.75h.008v.008H12v-.008Z" />
          </svg>
          <div>
            <p className="text-sm font-medium text-amber-800 dark:text-amber-200">
              AI processing timed out after {elapsed}s
            </p>
            <p className="mt-1 text-xs text-amber-600 dark:text-amber-400">
              The AI service may be unavailable or the model is still loading.
              The system will automatically retry processing in the background.
              Refresh the page later to check for results.
            </p>
            <button
              onClick={() => { setTimedOut(false); startRef.current = Date.now(); }}
              className="mt-2 text-xs font-medium text-amber-700 underline hover:text-amber-900 dark:text-amber-300 dark:hover:text-amber-100"
            >
              Retry polling
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (!aiProcessed) {
    return (
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-900">
        <h3 className="mb-4 text-sm font-semibold text-gray-900 dark:text-gray-100">
          AI Analysis
        </h3>
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <svg className="h-4 w-4 animate-spin text-gray-400" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
            <span className="text-sm text-gray-500">AI is processing this incident...</span>
          </div>
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-4 w-1/2" />
          <Skeleton className="h-20 w-full" />
        </div>
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-900">
      <h3 className="mb-4 text-sm font-semibold text-gray-900 dark:text-gray-100">
        AI Analysis
      </h3>
      <div className="space-y-3">
        <div className="flex flex-wrap gap-x-6 gap-y-2 text-sm">
          <div>
            <span className="text-gray-500">Category:</span>{' '}
            <span className="font-medium text-gray-900 dark:text-gray-100">
              {category ?? '--'}
            </span>
          </div>
          <div>
            <span className="text-gray-500">Priority:</span>{' '}
            <span className="font-medium text-gray-900 dark:text-gray-100">
              {priority ?? '--'}
            </span>
          </div>
          <div>
            <span className="text-gray-500">Confidence:</span>{' '}
            <span className="font-medium text-gray-900 dark:text-gray-100">
              {confidenceScore != null ? `${(confidenceScore * 100).toFixed(0)}%` : '--'}
            </span>
          </div>
        </div>
        {suggestion && (
          <div className="rounded-lg border border-gray-100 bg-gray-50 p-4 dark:border-gray-800 dark:bg-gray-800/50">
            <p className="mb-1 text-xs font-medium uppercase tracking-wider text-gray-500">
              Resolution Suggestion
            </p>
            <div className="prose prose-sm dark:prose-invert max-w-none text-gray-700 dark:text-gray-300">
              <Markdown>{extractSuggestionText(suggestion)}</Markdown>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function CommentItem({ comment }: { comment: CommentResponse }) {
  return (
    <div className="border-b border-gray-100 py-4 last:border-b-0 dark:border-gray-800">
      <div className="flex items-center gap-2">
        <div className="flex h-6 w-6 items-center justify-center rounded-full bg-gray-200 text-xs font-medium text-gray-600 dark:bg-gray-700 dark:text-gray-300">
          {comment.author.fullName.charAt(0).toUpperCase()}
        </div>
        <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
          {comment.author.fullName}
        </span>
        <span className="text-xs text-gray-400">{formatDateTime(comment.createdAt)}</span>
      </div>
      <p className="mt-2 pl-8 text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
        {comment.body}
      </p>
    </div>
  );
}

function HistoryItem({ entry }: { entry: HistoryResponse }) {
  return (
    <div className="flex gap-3 py-2">
      <div className="mt-1 h-2 w-2 shrink-0 rounded-full bg-gray-300 dark:bg-gray-600" />
      <div className="flex-1 text-sm">
        <span className="font-medium text-gray-900 dark:text-gray-100">
          {entry.changedBy.fullName}
        </span>{' '}
        <span className="text-gray-500">changed</span>{' '}
        <span className="font-medium text-gray-700 dark:text-gray-300">
          {entry.fieldChanged}
        </span>
        {entry.oldValue && (
          <>
            {' '}
            <span className="text-gray-400">from</span>{' '}
            <span className="text-gray-500">{entry.oldValue}</span>
          </>
        )}
        {entry.newValue && (
          <>
            {' '}
            <span className="text-gray-400">to</span>{' '}
            <span className="text-gray-700 dark:text-gray-300">{entry.newValue}</span>
          </>
        )}
        <span className="ml-2 text-xs text-gray-400">{formatDateTime(entry.changedAt)}</span>
      </div>
    </div>
  );
}

export function IncidentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const canModify = user?.role === UserRole.ENGINEER || user?.role === UserRole.ADMIN;
  const canDelete = user?.role === UserRole.ADMIN;
  const canComment = user?.role !== UserRole.VIEWER;

  const [commentText, setCommentText] = useState('');
  const [resolveOpen, setResolveOpen] = useState(false);
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [commentPage, setCommentPage] = useState(0);
  const [historyPage, setHistoryPage] = useState(0);

  const { data: incident, isLoading } = useQuery({
    queryKey: ['incident', id],
    queryFn: () => incidentsApi.get(id!).then((r) => r.data),
    enabled: !!id,
  });

  const { data: commentsData } = useQuery({
    queryKey: ['incident', id, 'comments', commentPage],
    queryFn: () => incidentsApi.getComments(id!, commentPage).then((r) => r.data),
    enabled: !!id,
  });

  const { data: historyData } = useQuery({
    queryKey: ['incident', id, 'history', historyPage],
    queryFn: () => incidentsApi.getHistory(id!, historyPage).then((r) => r.data),
    enabled: !!id,
  });

  const addCommentMutation = useMutation({
    mutationFn: (body: string) => incidentsApi.addComment(id!, { body }),
    onSuccess: () => {
      setCommentText('');
      void queryClient.invalidateQueries({ queryKey: ['incident', id, 'comments'] });
      toast('success', 'Comment added');
    },
    onError: () => toast('error', 'Failed to add comment'),
  });

  const resolveMutation = useMutation({
    mutationFn: (notes: string) => incidentsApi.resolve(id!, { resolutionNotes: notes }),
    onSuccess: () => {
      setResolveOpen(false);
      setResolutionNotes('');
      void queryClient.invalidateQueries({ queryKey: ['incident', id] });
      void queryClient.invalidateQueries({ queryKey: ['incidents'] });
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      toast('success', 'Incident resolved');
    },
    onError: () => toast('error', 'Failed to resolve incident'),
  });

  const deleteMutation = useMutation({
    mutationFn: () => incidentsApi.delete(id!),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['incidents'] });
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      toast('success', 'Incident deleted');
      navigate('/incidents');
    },
    onError: () => toast('error', 'Failed to delete incident'),
  });

  function handleCommentSubmit(e: FormEvent) {
    e.preventDefault();
    if (!commentText.trim()) return;
    addCommentMutation.mutate(commentText.trim());
  }

  function handleResolve(e: FormEvent) {
    e.preventDefault();
    if (resolutionNotes.trim().length < 10) return;
    resolveMutation.mutate(resolutionNotes.trim());
  }

  if (isLoading || !incident) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  const showResolve =
    canModify &&
    (incident.status === IncidentStatus.IN_PROGRESS || incident.status === IncidentStatus.OPEN);

  return (
    <div className="mx-auto max-w-4xl">
      {/* Header */}
      <div className="mb-6">
        <button
          onClick={() => navigate('/incidents')}
          className="mb-4 flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 dark:hover:text-gray-300"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
          </svg>
          Back to incidents
        </button>

        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex-1">
            <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
              {incident.title}
            </h1>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <Badge variant="status" value={incident.status} />
              <Badge variant="priority" value={incident.priority} />
              {incident.category && (
                <span className="rounded-md border border-gray-200 bg-gray-50 px-2 py-0.5 text-xs text-gray-600 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400">
                  {incident.category}
                </span>
              )}
            </div>
          </div>

          <div className="flex gap-2">
            {showResolve && (
              <Button size="sm" onClick={() => setResolveOpen(true)}>
                Resolve
              </Button>
            )}
            {canDelete && (
              <Button
                variant="danger"
                size="sm"
                loading={deleteMutation.isPending}
                onClick={() => {
                  if (window.confirm('Are you sure you want to delete this incident?')) {
                    deleteMutation.mutate();
                  }
                }}
              >
                Delete
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* Details */}
      <div className="space-y-6">
        {/* Description */}
        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-900">
          <h3 className="mb-3 text-sm font-semibold text-gray-900 dark:text-gray-100">
            Description
          </h3>
          <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
            {incident.description}
          </p>
          <div className="mt-4 flex flex-wrap gap-x-6 gap-y-2 border-t border-gray-100 pt-4 text-xs text-gray-500 dark:border-gray-800">
            <span>Reporter: {incident.reporter.fullName}</span>
            {incident.assignee && <span>Assignee: {incident.assignee.fullName}</span>}
            <span>Created: {formatDateTime(incident.createdAt)}</span>
            <span>Updated: {formatDateTime(incident.updatedAt)}</span>
            {incident.resolvedAt && <span>Resolved: {formatDateTime(incident.resolvedAt)}</span>}
          </div>
        </div>

        {/* AI Analysis */}
        <AiSection
          incidentId={incident.id}
          aiProcessed={incident.aiProcessed}
          category={incident.category}
          priority={incident.priority}
          confidenceScore={incident.aiConfidenceScore}
          suggestion={incident.aiResolutionSuggestion}
        />

        {/* Comments */}
        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-900">
          <h3 className="mb-4 text-sm font-semibold text-gray-900 dark:text-gray-100">
            Comments
            {commentsData && commentsData.totalElements > 0 && (
              <span className="ml-2 text-xs font-normal text-gray-400">
                ({commentsData.totalElements})
              </span>
            )}
          </h3>

          {canComment && (
            <form onSubmit={handleCommentSubmit} className="mb-4">
              <Textarea
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                placeholder="Add a comment..."
                rows={3}
              />
              <div className="mt-2 flex justify-end">
                <Button
                  size="sm"
                  type="submit"
                  loading={addCommentMutation.isPending}
                  disabled={!commentText.trim()}
                >
                  Comment
                </Button>
              </div>
            </form>
          )}

          {commentsData && commentsData.content.length > 0 ? (
            <>
              {commentsData.content.map((c) => (
                <CommentItem key={c.id} comment={c} />
              ))}
              <Pagination
                page={commentsData.page}
                totalPages={commentsData.totalPages}
                onPageChange={setCommentPage}
              />
            </>
          ) : (
            <p className="py-4 text-center text-sm text-gray-400">No comments yet</p>
          )}
        </div>

        {/* History */}
        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-900">
          <h3 className="mb-4 text-sm font-semibold text-gray-900 dark:text-gray-100">
            History
          </h3>
          {historyData && historyData.content.length > 0 ? (
            <>
              {historyData.content.map((h) => (
                <HistoryItem key={h.id} entry={h} />
              ))}
              <Pagination
                page={historyData.page}
                totalPages={historyData.totalPages}
                onPageChange={setHistoryPage}
              />
            </>
          ) : (
            <p className="py-4 text-center text-sm text-gray-400">No history yet</p>
          )}
        </div>
      </div>

      {/* Resolve Dialog */}
      <Dialog open={resolveOpen} onClose={() => setResolveOpen(false)} title="Resolve Incident">
        <form onSubmit={handleResolve} className="space-y-4">
          <Textarea
            label="Resolution Notes"
            value={resolutionNotes}
            onChange={(e) => setResolutionNotes(e.target.value)}
            placeholder="Describe how the incident was resolved (min 10 characters)..."
            rows={4}
            error={
              resolutionNotes.trim().length > 0 && resolutionNotes.trim().length < 10
                ? 'Must be at least 10 characters'
                : undefined
            }
          />
          <div className="flex justify-end gap-3">
            <Button type="button" variant="secondary" onClick={() => setResolveOpen(false)}>
              Cancel
            </Button>
            <Button
              type="submit"
              loading={resolveMutation.isPending}
              disabled={resolutionNotes.trim().length < 10}
            >
              Resolve Incident
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
