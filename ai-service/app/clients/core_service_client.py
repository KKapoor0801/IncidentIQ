import httpx
import structlog
from tenacity import (
    retry,
    retry_if_exception_type,
    stop_after_attempt,
    wait_exponential,
)

from app.core.config import settings
from app.models.ai_result import AiClassificationResult

logger = structlog.get_logger()


class CoreServiceClient:
    def __init__(self) -> None:
        self._base_url = settings.core_service_url
        self._token = settings.ai_service_token

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        retry=retry_if_exception_type(
            (httpx.ConnectError, httpx.TimeoutException)
        ),
        reraise=True,
    )
    async def send_ai_result(self, result: AiClassificationResult) -> None:
        url = (
            f"{self._base_url}/internal/api/v1/incidents/"
            f"{result.incident_id}/ai-result"
        )

        payload = {
            "incidentId": str(result.incident_id),
            "category": result.category,
            "priority": result.priority,
            "resolutionSuggestion": result.resolution_suggestion,
            "confidenceScore": result.confidence_score,
            "modelUsed": result.model_used,
            "processedAt": result.processed_at.isoformat(),
        }

        logger.debug("sending_ai_result_callback", incident_id=str(result.incident_id))

        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.patch(
                url,
                json=payload,
                headers={
                    "X-Internal-Token": self._token,
                    "Content-Type": "application/json",
                },
            )
            if not resp.is_success:
                logger.error("ai_result_callback_failed", incident_id=str(result.incident_id), status_code=resp.status_code)
            resp.raise_for_status()

        logger.info(
            "ai_result_callback_sent",
            incident_id=str(result.incident_id),
            category=result.category,
            priority=result.priority,
        )


core_service_client = CoreServiceClient()
