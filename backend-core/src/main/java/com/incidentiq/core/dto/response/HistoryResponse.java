package com.incidentiq.core.dto.response;

import java.time.Instant;
import java.util.UUID;

public record HistoryResponse(
        UUID id,
        String fieldChanged,
        String oldValue,
        String newValue,
        UserSummary changedBy,
        Instant changedAt
) {}
