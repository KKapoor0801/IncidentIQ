package com.incidentiq.core.domain.enums;

import java.util.Map;
import java.util.Set;

public final class IncidentStatusTransition {

    private IncidentStatusTransition() {}

    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED_TRANSITIONS = Map.of(
            IncidentStatus.OPEN, Set.of(IncidentStatus.IN_PROGRESS),
            IncidentStatus.IN_PROGRESS, Set.of(IncidentStatus.RESOLVED, IncidentStatus.OPEN),
            IncidentStatus.RESOLVED, Set.of(IncidentStatus.CLOSED, IncidentStatus.IN_PROGRESS),
            IncidentStatus.CLOSED, Set.of()
    );

    public static boolean isAllowed(IncidentStatus from, IncidentStatus to) {
        if (from == to) return true;
        Set<IncidentStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public static boolean isBackwardTransition(IncidentStatus from, IncidentStatus to) {
        return (from == IncidentStatus.IN_PROGRESS && to == IncidentStatus.OPEN);
    }
}
