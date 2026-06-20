package com.incidentiq.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(
        @NotBlank @Size(min = 3, max = 255) String title,
        @NotBlank @Size(min = 10, max = 5000) String description
) {}
