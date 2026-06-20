package com.incidentiq.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch configuration for keyword-based search.
 *
 * <p>Enables Spring Data Elasticsearch repositories scoped to the
 * {@code com.incidentiq.core.repository.elasticsearch} package.
 * Connection properties (URIs, timeouts) are configured via
 * {@code spring.data.elasticsearch.*} in application.yml.</p>
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.incidentiq.core.repository.elasticsearch")
public class ElasticsearchConfig {
}
