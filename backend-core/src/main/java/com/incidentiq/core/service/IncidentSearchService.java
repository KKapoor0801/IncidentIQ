package com.incidentiq.core.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.incidentiq.core.dto.response.PageResponse;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IncidentSearchService {

    private static final Logger log = LoggerFactory.getLogger(IncidentSearchService.class);

    private final ElasticsearchClient esClient;

    public IncidentSearchService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public PageResponse<Map<String, Object>> searchIncidents(String queryText, int page, int size) {
        return executeSearch(queryText, ElasticsearchIndexService.INCIDENTS_INDEX,
                List.of("title^2", "description"), page, size);
    }

    public PageResponse<Map<String, Object>> searchRunbooks(String queryText, int page, int size) {
        return executeSearch(queryText, ElasticsearchIndexService.RUNBOOKS_INDEX,
                List.of("title^2", "body"), page, size);
    }

    @SuppressWarnings("unchecked")
    private PageResponse<Map<String, Object>> executeSearch(String queryText, String indexName,
                                                             List<String> fields, int page, int size) {
        try {
            SearchRequest request = SearchRequest.of(s -> s
                    .index(indexName)
                    .from(page * size)
                    .size(size)
                    .query(q -> q
                            .multiMatch(mm -> mm
                                    .query(queryText)
                                    .fields(fields)
                            )
                    )
            );

            SearchResponse<ObjectNode> response = esClient.search(request, ObjectNode.class);

            List<Map<String, Object>> content = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(java.util.Objects::nonNull)
                    .map(node -> {
                        Map<String, Object> map = new HashMap<>();
                        node.fields().forEachRemaining(entry ->
                                map.put(entry.getKey(), entry.getValue().isTextual() ? entry.getValue().asText() : entry.getValue().toString()));
                        return map;
                    })
                    .toList();

            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;

            return new PageResponse<>(content, page, size, total, totalPages);
        } catch (Exception e) {
            log.warn("Elasticsearch search failed on index {}: {}", indexName, e.getMessage());
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }
    }
}
