import { clsx } from 'clsx';
import { STATUS_COLORS, PRIORITY_COLORS } from '@/lib/constants';
import { type IncidentStatus, type IncidentPriority } from '@/types';

interface StatusBadgeProps {
  variant: 'status';
  value: IncidentStatus;
}

interface PriorityBadgeProps {
  variant: 'priority';
  value: IncidentPriority;
}

type BadgeProps = StatusBadgeProps | PriorityBadgeProps;

export function Badge({ variant, value }: BadgeProps) {
  if (value == null) return null;

  const config = variant === 'status'
    ? STATUS_COLORS[value as IncidentStatus]
    : PRIORITY_COLORS[value as IncidentPriority];

  if (!config) return null;

  return (
    <span
      className={clsx(
        'inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-medium',
        config.bg,
        config.text,
      )}
    >
      {config.label}
    </span>
  );
}
