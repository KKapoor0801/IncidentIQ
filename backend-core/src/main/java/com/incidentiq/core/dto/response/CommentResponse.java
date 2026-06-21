package com.incidentiq.core.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UserSummary author,
        String body,
        Instant createdAt
) {}
