import json
import time

import httpx
import structlog
from tenacity import (
    retry,
    retry_if_exception_type,
    stop_after_attempt,
    wait_exponential,
)

from app.core.config import settings

logger = structlog.get_logger()


class OllamaTimeoutError(Exception):
    pass


class OllamaClient:
    def __init__(self) -> None:
        self._base_url = settings.ollama_base_url
        self._model = settings.ollama_model
        self._timeout = settings.ollama_timeout_ms / 1000.0

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        retry=retry_if_exception_type((OllamaTimeoutError, httpx.ConnectError)),
        reraise=True,
    )
    async def generate(
        self, prompt: str, system_prompt: str = "", json_format: bool = False,
    ) -> str:
        logger.debug("ollama_request_starting", model=self._model)
        start = time.monotonic()
        try:
            async with httpx.AsyncClient(timeout=self._timeout) as client:
                payload: dict[str, object] = {
                    "model": self._model,
                    "prompt": prompt,
                    "stream": False,
                }
                if json_format:
                    payload["format"] = "json"
                if system_prompt:
                    payload["system"] = system_prompt

                response = await client.post(
                    f"{self._base_url}/api/generate",
                    json=payload,
                )
                response.raise_for_status()
                result = response.json()
                latency_ms = (time.monotonic() - start) * 1000

                logger.info(
                    "ollama_inference",
                    model=self._model,
                    latency_ms=round(latency_ms, 1),
                )

                return str(result.get("response", ""))

        except httpx.TimeoutException as e:
            latency_ms = (time.monotonic() - start) * 1000
            logger.error(
                "ollama_timeout",
                model=self._model,
                latency_ms=round(latency_ms, 1),
            )
            raise OllamaTimeoutError(
                f"Ollama timeout after {round(latency_ms)}ms"
            ) from e

    async def generate_json(
        self, prompt: str, system_prompt: str = "",
    ) -> dict[str, object]:
        raw = await self.generate(prompt, system_prompt, json_format=True)
        try:
            return dict(json.loads(raw))
        except json.JSONDecodeError:
            logger.warning("ollama_json_parse_failed", raw_response=raw[:200])
            return {}


ollama_client = OllamaClient()
