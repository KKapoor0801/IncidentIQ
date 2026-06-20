package com.incidentiq.core.dto.request;

import com.incidentiq.core.domain.enums.IncidentStatus;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateIncidentRequest(
        @Size(min = 3, max = 255) String title,
        @Size(min = 10, max = 5000) String description,
        IncidentStatus status,
        UUID assigneeId
) {}
