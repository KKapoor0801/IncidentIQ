package com.incidentiq.core.kafka.producer;

import com.incidentiq.core.config.KafkaConfig;
import com.incidentiq.core.domain.entity.Incident;
import com.incidentiq.core.dto.event.IncidentAiCompletedEvent;
import com.incidentiq.core.dto.event.IncidentCreatedEvent;
import com.incidentiq.core.dto.event.IncidentUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class IncidentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(IncidentEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public IncidentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCreated(Incident incident) {
        var event = IncidentCreatedEvent.from(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getReporter().getId(),
                incident.getCreatedAt(),
                resolveTraceId());

        String key = incident.getId().toString();
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_INCIDENT_CREATED, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish incident.created for {}: {}", key, ex.getMessage());
                        } else {
                            log.info("Published incident.created for {} to partition {}",
                                    key, result.getRecordMetadata().partition());
                        }
                    });
        } catch (Exception e) {
            log.error("Kafka publish failed for incident.created {}: {}", key, e.getMessage());
        }
    }

    public void publishUpdated(Incident incident, List<String> changedFields) {
        var event = IncidentUpdatedEvent.from(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                changedFields,
                incident.getUpdatedAt(),
                resolveTraceId());

        String key = incident.getId().toString();
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_INCIDENT_UPDATED, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish incident.updated for {}: {}", key, ex.getMessage());
                        } else {
                            log.info("Published incident.updated for {} (reprocess={}) to partition {}",
                                    key, event.requiresReprocessing(), result.getRecordMetadata().partition());
                        }
                    });
        } catch (Exception e) {
            log.error("Kafka publish failed for incident.updated {}: {}", key, e.getMessage());
        }
    }

    public void publishAiCompleted(Incident incident) {
        var event = IncidentAiCompletedEvent.from(
                incident.getId(),
                incident.getCategory() != null ? incident.getCategory().name() : null,
                incident.getPriority() != null ? incident.getPriority().name() : null,
                incident.getAiConfidenceScore(),
                "llama3.1:8b",
                resolveTraceId());

        String key = incident.getId().toString();
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_INCIDENT_AI_COMPLETED, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish incident.ai.completed for {}: {}", key, ex.getMessage());
                        } else {
                            log.info("Published incident.ai.completed for {}", key);
                        }
                    });
        } catch (Exception e) {
            log.error("Kafka publish failed for incident.ai.completed {}: {}", key, e.getMessage());
        }
    }

    private String resolveTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }
}
