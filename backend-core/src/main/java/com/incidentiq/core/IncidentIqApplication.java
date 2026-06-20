package com.incidentiq.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the IncidentIQ Core Service.
 */
@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.incidentiq.core.config")
@EnableScheduling
public class IncidentIqApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentIqApplication.class, args);
    }
}
