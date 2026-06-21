import json
from datetime import UTC, datetime
from uuid import UUID, uuid4

import structlog
from aiokafka import AIOKafkaProducer

from app.core.config import settings

logger = structlog.get_logger()

_producer: AIOKafkaProducer | None = None


async def get_dlq_producer() -> AIOKafkaProducer:
    global _producer  # noqa: PLW0603
    if _producer is None:
        _producer = AIOKafkaProducer(
            bootstrap_servers=settings.kafka_bootstrap_servers,
            value_serializer=lambda v: json.dumps(v).encode("utf-8"),
            key_serializer=lambda k: k.encode("utf-8") if k else None,
        )
        await _producer.start()
    return _producer


async def publish_to_dlq(
    incident_id: UUID,
    original_topic: str,
    error_message: str,
    retry_count: int,
) -> None:
    try:
        producer = await get_dlq_producer()
        event = {
            "eventId": str(uuid4()),
            "originalTopic": original_topic,
            "incidentId": str(incident_id),
            "errorMessage": error_message,
            "retryCount": retry_count,
            "failedAt": datetime.now(UTC).isoformat(),
        }
        await producer.send(
            "incident.ai.dlq",
            key=str(incident_id),
            value=event,
        )
        logger.info(
            "published_to_dlq",
            incident_id=str(incident_id),
            original_topic=original_topic,
            error=error_message,
        )
    except Exception as e:
        logger.error(
            "dlq_publish_failed",
            incident_id=str(incident_id),
            error=str(e),
        )
