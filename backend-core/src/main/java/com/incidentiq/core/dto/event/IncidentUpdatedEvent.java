package com.incidentiq.core.dto.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentUpdatedEvent(
        UUID eventId,
        String eventType,
        UUID incidentId,
        List<String> changedFields,
        String title,
        String description,
        boolean requiresReprocessing,
        Instant updatedAt,
        String schemaVersion
) {
    public static IncidentUpdatedEvent from(UUID incidentId, String title, String description,
                                             List<String> changedFields, Instant updatedAt) {
        boolean reprocess = changedFields.contains("title") || changedFields.contains("description");
        return new IncidentUpdatedEvent(
                UUID.randomUUID(), "INCIDENT_UPDATED", incidentId, changedFields,
                title, description, reprocess, updatedAt, "1.0");
    }
}
