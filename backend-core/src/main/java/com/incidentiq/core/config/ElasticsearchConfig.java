package com.incidentiq.core.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Value("${spring.data.elasticsearch.uris:http://localhost:9200}")
    private String esUris;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        String cleaned = esUris.replace("http://", "").replace("https://", "");
        String[] parts = cleaned.split(":");
        String hostname = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;

        Rest5Client restClient = Rest5Client.builder(new HttpHost("http", hostname, port)).build();
        Rest5ClientTransport transport = new Rest5ClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
