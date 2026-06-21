import asyncio
import json
from datetime import UTC, datetime
from uuid import UUID

import structlog
import structlog.contextvars
from aiokafka import AIOKafkaConsumer

from app.clients.core_service_client import core_service_client
from app.clients.ollama_client import OllamaTimeoutError
from app.core.config import settings
from app.models.ai_result import AiClassificationResult
from app.models.incident_event import (
    IncidentCreatedEvent,
    IncidentUpdatedEvent,
)
from app.services.categorization_service import categorize
from app.services.dlq_service import publish_to_dlq
from app.services.prioritization_service import prioritize
from app.services.resolution_suggestion_service import suggest_resolution

logger = structlog.get_logger()

TOPICS = ["incident.created", "incident.updated"]


async def start_consumer() -> None:
    consumer = AIOKafkaConsumer(
        *TOPICS,
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_consumer_group,
        auto_offset_reset="earliest",
        value_deserializer=lambda v: json.loads(v.decode("utf-8")),
        key_deserializer=lambda k: k.decode("utf-8") if k else None,
    )

    while True:
        try:
            await consumer.start()
            logger.info(
                "kafka_consumer_started",
                topics=TOPICS,
                group=settings.kafka_consumer_group,
            )
            break
        except Exception as e:
            logger.warning("kafka_consumer_connect_retry", error=str(e))
            await asyncio.sleep(5)

    try:
        async for msg in consumer:
            try:
                await process_message(msg.topic, msg.value, msg.key)
            except Exception as e:
                logger.error(
                    "message_processing_failed",
                    topic=msg.topic,
                    key=msg.key,
                    error=str(e),
                )
    finally:
        await consumer.stop()
        logger.info("kafka_consumer_stopped")


async def process_message(
    topic: str, value: dict[str, object], key: str | None,
) -> None:
    trace_id = value.get("traceId")
    if trace_id is not None:
        structlog.contextvars.bind_contextvars(trace_id=trace_id)

    try:
        if topic == "incident.created":
            event = IncidentCreatedEvent(**value)
            logger.info(
                "processing_incident_created",
                incident_id=str(event.incident_id),
            )
            await classify_and_callback(
                event.incident_id, event.title, event.description, topic,
            )

        elif topic == "incident.updated":
            event = IncidentUpdatedEvent(**value)
            if not event.requires_reprocessing:
                logger.info(
                    "skipping_incident_updated",
                    incident_id=str(event.incident_id),
                    reason="requiresReprocessing=false",
                )
                return
            logger.info(
                "processing_incident_updated",
                incident_id=str(event.incident_id),
            )
            await classify_and_callback(
                event.incident_id, event.title, event.description, topic,
            )
    finally:
        structlog.contextvars.unbind_contextvars("trace_id")


async def classify_and_callback(
    incident_id: UUID, title: str, description: str, source_topic: str,
) -> None:
    try:
        category = await categorize(title, description)
        priority, confidence = await prioritize(title, description, category)
        resolution = await suggest_resolution(title, description)

        result = AiClassificationResult(
            incident_id=incident_id,
            category=category,
            priority=priority,
            confidence_score=confidence,
            resolution_suggestion=resolution,
            model_used=settings.ollama_model,
            processed_at=datetime.now(UTC),
        )

        logger.info(
            "classification_complete",
            incident_id=str(incident_id),
            category=result.category,
            priority=result.priority,
            confidence=result.confidence_score,
        )

        await core_service_client.send_ai_result(result)

    except OllamaTimeoutError as e:
        logger.error(
            "ollama_exhausted",
            incident_id=str(incident_id),
            error=str(e),
        )
        await publish_to_dlq(
            incident_id=incident_id,
            original_topic=source_topic,
            error_message=str(e),
            retry_count=3,
        )

    except Exception as e:
        logger.error(
            "classification_or_callback_failed",
            incident_id=str(incident_id),
            error=str(e),
        )
        await publish_to_dlq(
            incident_id=incident_id,
            original_topic=source_topic,
            error_message=str(e),
            retry_count=3,
        )
