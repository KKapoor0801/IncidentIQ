package com.incidentiq.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveIncidentRequest(
        @NotBlank @Size(min = 10, max = 5000) String resolutionNotes
) {}
