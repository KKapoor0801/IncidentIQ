package com.incidentiq.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration for the IncidentIQ REST API.
 *
 * <p>Configures the public API documentation with JWT bearer authentication
 * as the security scheme. Internal endpoints ({@code /internal/**}) are
 * excluded via {@code springdoc.paths-to-exclude} in application.yml.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Defines the OpenAPI specification metadata and security scheme.
     *
     * @return the configured {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI incidentIqOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("IncidentIQ API")
                        .description("AI-powered Incident Intelligence Platform — REST API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("IncidentIQ Engineering")
                                .email("engineering@incidentiq.dev")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
