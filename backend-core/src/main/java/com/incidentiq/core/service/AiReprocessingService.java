package com.incidentiq.core.service;

import com.incidentiq.core.domain.entity.Incident;
import com.incidentiq.core.kafka.producer.IncidentEventProducer;
import com.incidentiq.core.repository.jpa.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AiReprocessingService {

    private static final Logger log = LoggerFactory.getLogger(AiReprocessingService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentEventProducer eventProducer;

    public AiReprocessingService(IncidentRepository incidentRepository,
                                  IncidentEventProducer eventProducer) {
        this.incidentRepository = incidentRepository;
        this.eventProducer = eventProducer;
    }

    @Scheduled(fixedRate = 300_000)
    public void reprocessFailedIncidents() {
        Instant threshold = Instant.now().minus(2, ChronoUnit.MINUTES);
        List<Incident> unprocessed = incidentRepository.findUnprocessedBefore(threshold);

        if (unprocessed.isEmpty()) {
            return;
        }

        log.info("Reprocessing {} unprocessed incidents", unprocessed.size());
        for (Incident incident : unprocessed) {
            try {
                eventProducer.publishCreated(incident);
                log.info("Re-published incident.created for reprocessing: {}", incident.getId());
            } catch (Exception e) {
                log.error("Failed to re-publish incident {}: {}", incident.getId(), e.getMessage());
            }
        }
    }
}
