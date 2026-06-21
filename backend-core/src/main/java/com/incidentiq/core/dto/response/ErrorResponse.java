package com.incidentiq.core.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}

    public static ErrorResponse of(int status, String error, String message, String path, String traceId) {
        return new ErrorResponse(Instant.now(), status, error, message, path, traceId, null);
    }

    public static ErrorResponse withFieldErrors(int status, String error, String message, String path, String traceId, List<FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, message, path, traceId, fieldErrors);
    }
}
