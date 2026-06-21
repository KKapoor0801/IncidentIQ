import { useQuery } from '@tanstack/react-query';
import { dashboardApi } from '@/api/dashboard';
import { Skeleton } from '@/components/ui/Skeleton';

interface StatCardProps {
  label: string;
  value: number;
  color: string;
}

function StatCard({ label, value, color }: StatCardProps) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-900">
      <p className="text-sm font-medium text-gray-500 dark:text-gray-400">{label}</p>
      <p className={`mt-2 text-3xl font-semibold ${color}`}>{value}</p>
    </div>
  );
}

function StatSkeleton() {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-900">
      <Skeleton className="h-4 w-24" />
      <Skeleton className="mt-3 h-8 w-16" />
    </div>
  );
}

export function DashboardPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['dashboard', 'summary'],
    queryFn: () => dashboardApi.getSummary().then((r) => r.data),
  });

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-semibold text-gray-900 dark:text-gray-100">
          Dashboard
        </h1>
        <p className="mt-1 text-sm text-gray-500">
          Overview of your incident landscape
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {isLoading ? (
          <>
            <StatSkeleton />
            <StatSkeleton />
            <StatSkeleton />
            <StatSkeleton />
          </>
        ) : data ? (
          <>
            <StatCard
              label="Open Incidents"
              value={data.openCount}
              color="text-blue-600 dark:text-blue-400"
            />
            <StatCard
              label="In Progress"
              value={data.inProgressCount}
              color="text-purple-600 dark:text-purple-400"
            />
            <StatCard
              label="Resolved"
              value={data.resolvedCount}
              color="text-green-600 dark:text-green-400"
            />
            <StatCard
              label="P1 Critical"
              value={data.p1Count}
              color="text-red-600 dark:text-red-400"
            />
          </>
        ) : null}
      </div>
    </div>
  );
}
