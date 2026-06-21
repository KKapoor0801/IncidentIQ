package com.incidentiq.core.dto.response;

public record DashboardSummary(
        long openCount,
        long inProgressCount,
        long resolvedCount,
        long p1Count
) {}
