package com.incidentiq.core.dto.response;

import com.incidentiq.core.domain.enums.IncidentCategory;
import com.incidentiq.core.domain.enums.IncidentPriority;
import com.incidentiq.core.domain.enums.IncidentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        String title,
        String description,
        IncidentStatus status,
        IncidentPriority priority,
        IncidentCategory category,
        String aiResolutionSuggestion,
        BigDecimal aiConfidenceScore,
        boolean aiProcessed,
        UserSummary reporter,
        UserSummary assignee,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt
) {}
