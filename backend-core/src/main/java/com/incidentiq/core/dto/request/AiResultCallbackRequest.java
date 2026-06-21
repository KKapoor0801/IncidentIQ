package com.incidentiq.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AiResultCallbackRequest(
        @NotNull UUID incidentId,
        @NotBlank String category,
        @NotBlank String priority,
        String resolutionSuggestion,
        @NotNull BigDecimal confidenceScore,
        @NotBlank String modelUsed,
        @NotNull Instant processedAt
) {}
