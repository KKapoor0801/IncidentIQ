package com.incidentiq.core.exception;

public class IncidentIqException extends RuntimeException {

    public IncidentIqException(String message) {
        super(message);
    }

    public IncidentIqException(String message, Throwable cause) {
        super(message, cause);
    }
}
