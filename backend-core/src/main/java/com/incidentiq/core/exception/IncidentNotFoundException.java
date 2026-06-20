package com.incidentiq.core.exception;

import java.util.UUID;

public class IncidentNotFoundException extends IncidentIqException {

    public IncidentNotFoundException(UUID incidentId) {
        super("Incident with id " + incidentId + " was not found");
    }
}
