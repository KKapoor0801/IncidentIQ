package com.incidentiq.core.dto.response;

import java.util.UUID;

public record UserSummary(
        UUID id,
        String fullName,
        String email
) {}
