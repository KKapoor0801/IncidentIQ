package com.incidentiq.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for the IncidentIQ Core Service.
 *
 * <p>Configures CORS for the React SPA frontend using the allowed origins
 * defined in {@link CorsProperties}. No wildcard origins are permitted
 * in production per SDD Section 30.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    /**
     * Constructs the web configuration with the required CORS properties.
     *
     * @param corsProperties the CORS configuration properties
     */
    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = corsProperties.allowedOrigins().split(",");

        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
