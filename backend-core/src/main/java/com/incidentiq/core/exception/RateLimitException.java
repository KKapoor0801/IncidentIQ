package com.incidentiq.core.exception;

public class RateLimitException extends IncidentIqException {

    public RateLimitException() {
        super("Rate limit exceeded. Please try again later.");
    }
}
