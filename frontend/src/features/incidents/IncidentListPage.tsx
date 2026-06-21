import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { incidentsApi } from '@/api/incidents';
import { IncidentStatus, IncidentPriority, UserRole, type IncidentResponse } from '@/types';
import { useAuthStore } from '@/store/authStore';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { Pagination } from '@/components/ui/Pagination';
import { CreateIncidentDialog } from './CreateIncidentDialog';
import { SearchModal } from './SearchModal';

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

function IncidentRow({ incident }: { incident: IncidentResponse }) {
  return (
    <Link
      to={`/incidents/${incident.id}`}
      className="block border-b border-gray-100 px-4 py-3 transition-colors hover:bg-gray-50 last:border-b-0 dark:border-gray-800 dark:hover:bg-gray-800/50"
    >
      {/* Desktop row */}
      <div className="hidden items-center gap-4 md:flex">
        <span className="flex-1 truncate text-sm font-medium text-gray-900 dark:text-gray-100">
          {incident.title}
        </span>
        <Badge variant="status" value={incident.status} />
        <Badge variant="priority" value={incident.priority} />
        <span className="w-20 text-xs text-gray-500 dark:text-gray-400">
          {incident.category ?? '--'}
        </span>
        <span className="w-28 truncate text-xs text-gray-500 dark:text-gray-400">
          {incident.reporter.fullName}
        </span>
        <span className="w-24 text-xs text-gray-400">
          {formatDate(incident.createdAt)}
        </span>
      </div>
      {/* Mobile card */}
      <div className="md:hidden">
        <div className="flex items-start justify-between gap-2">
          <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
            {incident.title}
          </span>
        </div>
        <div className="mt-2 flex flex-wrap items-center gap-2">
          <Badge variant="status" value={incident.status} />
          <Badge variant="priority" value={incident.priority} />
          <span className="text-xs text-gray-400">{formatDate(incident.createdAt)}</span>
        </div>
      </div>
    </Link>
  );
}

function TableSkeleton() {
  return (
    <div className="space-y-1">
      {Array.from({ length: 8 }).map((_, i) => (
        <div key={i} className="px-4 py-3">
          <Skeleton className="h-5 w-full" />
        </div>
      ))}
    </div>
  );
}

export function IncidentListPage() {
  const user = useAuthStore((s) => s.user);
  const canCreate = user?.role !== UserRole.VIEWER;
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<IncidentStatus | ''>('');
  const [priorityFilter, setPriorityFilter] = useState<IncidentPriority | ''>('');
  const [createOpen, setCreateOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: [
      'incidents',
      { page, status: statusFilter || undefined, priority: priorityFilter || undefined },
    ],
    queryFn: () =>
      incidentsApi
        .list({
          page,
          size: 20,
          status: statusFilter || undefined,
          priority: priorityFilter || undefined,
        })
        .then((r) => r.data),
  });

  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
      e.preventDefault();
      setSearchOpen(true);
    }
  }, []);

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900 dark:text-gray-100">
            Incidents
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            {data ? `${data.totalElements} total incidents` : 'Loading...'}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" onClick={() => setSearchOpen(true)}>
            <svg className="mr-1.5 h-4 w-4" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
            </svg>
            Search
            <kbd className="ml-2 hidden rounded border border-gray-200 px-1 text-xs text-gray-400 sm:inline dark:border-gray-700">
              {'⌘'}K
            </kbd>
          </Button>
          {canCreate && (
            <Button size="sm" onClick={() => setCreateOpen(true)}>
              Create Incident
            </Button>
          )}
        </div>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-wrap gap-3">
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value as IncidentStatus | '');
            setPage(0);
          }}
          className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-700 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300"
        >
          <option value="">All Statuses</option>
          {Object.values(IncidentStatus).map((s) => (
            <option key={s} value={s}>
              {s.replace('_', ' ')}
            </option>
          ))}
        </select>
        <select
          value={priorityFilter}
          onChange={(e) => {
            setPriorityFilter(e.target.value as IncidentPriority | '');
            setPage(0);
          }}
          className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-700 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300"
        >
          <option value="">All Priorities</option>
          {Object.values(IncidentPriority).map((p) => (
            <option key={p} value={p}>
              {p}
            </option>
          ))}
        </select>
      </div>

      {/* Table */}
      <div className="rounded-xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-900">
        {/* Desktop header */}
        <div className="hidden border-b border-gray-200 px-4 py-2 md:flex items-center gap-4 dark:border-gray-800">
          <span className="flex-1 text-xs font-medium uppercase tracking-wider text-gray-500">
            Title
          </span>
          <span className="w-24 text-xs font-medium uppercase tracking-wider text-gray-500">
            Status
          </span>
          <span className="w-20 text-xs font-medium uppercase tracking-wider text-gray-500">
            Priority
          </span>
          <span className="w-20 text-xs font-medium uppercase tracking-wider text-gray-500">
            Category
          </span>
          <span className="w-28 text-xs font-medium uppercase tracking-wider text-gray-500">
            Reporter
          </span>
          <span className="w-24 text-xs font-medium uppercase tracking-wider text-gray-500">
            Created
          </span>
        </div>

        {isLoading ? (
          <TableSkeleton />
        ) : data && data.content.length > 0 ? (
          data.content.map((incident) => (
            <IncidentRow key={incident.id} incident={incident} />
          ))
        ) : (
          <div className="px-4 py-12 text-center text-sm text-gray-400">
            No incidents found
          </div>
        )}
      </div>

      {data && (
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          onPageChange={setPage}
        />
      )}

      <CreateIncidentDialog open={createOpen} onClose={() => setCreateOpen(false)} />
      <SearchModal open={searchOpen} onClose={() => setSearchOpen(false)} />
    </div>
  );
}
