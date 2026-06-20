package com.incidentiq.core.dto.event;

import java.time.Instant;
import java.util.UUID;

public record IncidentCreatedEvent(
        UUID eventId,
        String eventType,
        UUID incidentId,
        String title,
        String description,
        UUID reporterId,
        Instant createdAt,
        String schemaVersion
) {
    public static IncidentCreatedEvent from(UUID incidentId, String title, String description,
                                             UUID reporterId, Instant createdAt) {
        return new IncidentCreatedEvent(
                UUID.randomUUID(), "INCIDENT_CREATED", incidentId, title, description,
                reporterId, createdAt, "1.0");
    }
}
