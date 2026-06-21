package com.incidentiq.core.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record IncidentAiCompletedEvent(
        UUID eventId,
        String eventType,
        UUID incidentId,
        String category,
        String priority,
        BigDecimal confidenceScore,
        String modelUsed,
        Instant completedAt,
        String schemaVersion,
        String traceId
) {
    public static IncidentAiCompletedEvent from(UUID incidentId, String category, String priority,
                                                 BigDecimal confidenceScore, String modelUsed, String traceId) {
        return new IncidentAiCompletedEvent(
                UUID.randomUUID(), "INCIDENT_AI_COMPLETED", incidentId, category, priority,
                confidenceScore, modelUsed, Instant.now(), "1.0", traceId);
    }
}
