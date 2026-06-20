package com.incidentiq.core;

import org.junit.jupiter.api.Test;

class IncidentIqApplicationTests {

    @Test
    void applicationClassLoads() {
        IncidentIqApplication application = new IncidentIqApplication();
        assert application != null;
    }
}
