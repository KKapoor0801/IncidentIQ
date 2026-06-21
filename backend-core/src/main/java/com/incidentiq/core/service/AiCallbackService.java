package com.incidentiq.core.service;

import com.incidentiq.core.domain.entity.Incident;
import com.incidentiq.core.domain.enums.IncidentCategory;
import com.incidentiq.core.domain.enums.IncidentPriority;
import com.incidentiq.core.dto.request.AiResultCallbackRequest;
import com.incidentiq.core.exception.IncidentNotFoundException;
import com.incidentiq.core.kafka.producer.IncidentEventProducer;
import com.incidentiq.core.repository.jpa.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AiCallbackService {

    private static final Logger log = LoggerFactory.getLogger(AiCallbackService.class);

    private final IncidentRepository incidentRepository;
    private final ElasticsearchIndexService esIndexService;
    private final IncidentCacheService cacheService;
    private final IncidentEventProducer eventProducer;

    public AiCallbackService(IncidentRepository incidentRepository,
                              ElasticsearchIndexService esIndexService,
                              IncidentCacheService cacheService,
                              IncidentEventProducer eventProducer) {
        this.incidentRepository = incidentRepository;
        this.esIndexService = esIndexService;
        this.cacheService = cacheService;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public void applyAiResult(UUID incidentId, AiResultCallbackRequest callback) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        incident.setCategory(IncidentCategory.valueOf(callback.category()));
        incident.setPriority(IncidentPriority.valueOf(callback.priority()));
        incident.setAiResolutionSuggestion(callback.resolutionSuggestion());
        incident.setAiConfidenceScore(callback.confidenceScore());
        incident.setAiProcessed(true);

        incident = incidentRepository.save(incident);

        esIndexService.indexIncident(incident);
        cacheService.onIncidentWrite(incidentId);

        try {
            eventProducer.publishAiCompleted(incident);
        } catch (Exception e) {
            log.error("Failed to publish incident.ai.completed for {}", incidentId, e);
        }

        log.info("AI result applied for incident {} — category={}, priority={}, confidence={}",
                incidentId, callback.category(), callback.priority(), callback.confidenceScore());
    }
}
