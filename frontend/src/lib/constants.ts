import { IncidentPriority, IncidentStatus } from '@/types';

interface ColorConfig {
  bg: string;
  text: string;
  label: string;
}

export const STATUS_COLORS: Record<IncidentStatus, ColorConfig> = {
  [IncidentStatus.OPEN]: {
    bg: 'bg-[var(--status-open-bg)] border-[var(--status-open-border)]',
    text: 'text-[var(--status-open-text)]',
    label: 'Open',
  },
  [IncidentStatus.IN_PROGRESS]: {
    bg: 'bg-[var(--status-in-progress-bg)] border-[var(--status-in-progress-border)]',
    text: 'text-[var(--status-in-progress-text)]',
    label: 'In Progress',
  },
  [IncidentStatus.RESOLVED]: {
    bg: 'bg-[var(--status-resolved-bg)] border-[var(--status-resolved-border)]',
    text: 'text-[var(--status-resolved-text)]',
    label: 'Resolved',
  },
  [IncidentStatus.CLOSED]: {
    bg: 'bg-[var(--status-closed-bg)] border-[var(--status-closed-border)]',
    text: 'text-[var(--status-closed-text)]',
    label: 'Closed',
  },
};

export const PRIORITY_COLORS: Record<IncidentPriority, ColorConfig> = {
  [IncidentPriority.P1]: {
    bg: 'bg-[var(--priority-p1-bg)] border-[var(--priority-p1-border)]',
    text: 'text-[var(--priority-p1-text)]',
    label: 'P1 - Critical',
  },
  [IncidentPriority.P2]: {
    bg: 'bg-[var(--priority-p2-bg)] border-[var(--priority-p2-border)]',
    text: 'text-[var(--priority-p2-text)]',
    label: 'P2 - High',
  },
  [IncidentPriority.P3]: {
    bg: 'bg-[var(--priority-p3-bg)] border-[var(--priority-p3-border)]',
    text: 'text-[var(--priority-p3-text)]',
    label: 'P3 - Medium',
  },
  [IncidentPriority.P4]: {
    bg: 'bg-[var(--priority-p4-bg)] border-[var(--priority-p4-border)]',
    text: 'text-[var(--priority-p4-text)]',
    label: 'P4 - Low',
  },
};
