package com.incidentiq.core.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.incidentiq.core.domain.entity.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ElasticsearchIndexService {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexService.class);

    public static final String INCIDENTS_INDEX = "incidents";
    public static final String RUNBOOKS_INDEX = "runbooks";
    public static final String SERVICE_METADATA_INDEX = "service_metadata";

    private final ElasticsearchClient esClient;

    public ElasticsearchIndexService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    @Async
    public void indexIncident(Incident incident) {
        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("id", incident.getId().toString());
            doc.put("title", incident.getTitle());
            doc.put("description", incident.getDescription());
            doc.put("status", incident.getStatus().name());
            doc.put("priority", incident.getPriority() != null ? incident.getPriority().name() : null);
            doc.put("category", incident.getCategory() != null ? incident.getCategory().name() : null);
            doc.put("reporterId", incident.getReporter() != null ? incident.getReporter().getId().toString() : null);
            doc.put("assigneeId", incident.getAssignee() != null ? incident.getAssignee().getId().toString() : null);
            doc.put("aiResolutionSuggestion", incident.getAiResolutionSuggestion());
            doc.put("createdAt", incident.getCreatedAt() != null ? incident.getCreatedAt().toString() : null);
            doc.put("updatedAt", incident.getUpdatedAt() != null ? incident.getUpdatedAt().toString() : null);
            doc.put("resolvedAt", incident.getResolvedAt() != null ? incident.getResolvedAt().toString() : null);

            esClient.index(i -> i
                    .index(INCIDENTS_INDEX)
                    .id(incident.getId().toString())
                    .document(doc)
            );
            log.debug("Indexed incident {} to Elasticsearch", incident.getId());
        } catch (Exception e) {
            log.error("Failed to index incident {} to Elasticsearch: {}", incident.getId(), e.getMessage());
        }
    }
}
