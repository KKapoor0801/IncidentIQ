package com.incidentiq.core.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic configuration for the IncidentIQ event-driven architecture.
 *
 * <p>Defines all Kafka topics as specified in the SDD Section 8.
 * Partition keys are set to {@code incidentId} at the producer level
 * to guarantee ordered processing per incident.</p>
 */
@Configuration
public class KafkaConfig {

    /** Topic for newly created incidents, consumed by the AI Intelligence Service. */
    public static final String TOPIC_INCIDENT_CREATED = "incident.created";

    /** Topic for updated incidents requiring AI reprocessing. */
    public static final String TOPIC_INCIDENT_UPDATED = "incident.updated";

    /** Topic published after AI classification results have been persisted. */
    public static final String TOPIC_INCIDENT_AI_COMPLETED = "incident.ai.completed";

    /** Dead letter queue for events that failed AI processing after retry exhaustion. */
    public static final String TOPIC_INCIDENT_AI_DLQ = "incident.ai.dlq";

    private static final int PRIMARY_PARTITIONS = 6;
    private static final int COMPLETION_PARTITIONS = 3;
    private static final int DLQ_PARTITIONS = 1;
    private static final int REPLICATION_FACTOR = 1;

    /**
     * Creates the {@code incident.created} topic.
     *
     * @return the topic configuration
     */
    @Bean
    public NewTopic incidentCreatedTopic() {
        return TopicBuilder.name(TOPIC_INCIDENT_CREATED)
                .partitions(PRIMARY_PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .build();
    }

    /**
     * Creates the {@code incident.updated} topic.
     *
     * @return the topic configuration
     */
    @Bean
    public NewTopic incidentUpdatedTopic() {
        return TopicBuilder.name(TOPIC_INCIDENT_UPDATED)
                .partitions(PRIMARY_PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .build();
    }

    /**
     * Creates the {@code incident.ai.completed} topic.
     *
     * @return the topic configuration
     */
    @Bean
    public NewTopic incidentAiCompletedTopic() {
        return TopicBuilder.name(TOPIC_INCIDENT_AI_COMPLETED)
                .partitions(COMPLETION_PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .build();
    }

    /**
     * Creates the {@code incident.ai.dlq} dead letter queue topic.
     *
     * @return the topic configuration
     */
    @Bean
    public NewTopic incidentAiDlqTopic() {
        return TopicBuilder.name(TOPIC_INCIDENT_AI_DLQ)
                .partitions(DLQ_PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .config("retention.ms", String.valueOf(14 * 24 * 60 * 60 * 1000L))
                .build();
    }
}
