from typing import Any

import httpx
import structlog

from app.core.config import settings

logger = structlog.get_logger()


class ElasticsearchClient:
    def __init__(self) -> None:
        self._base_url = settings.es_uris

    async def search_runbooks(
        self, query_text: str, top_k: int = 3
    ) -> list[dict[str, Any]]:
        return await self._multi_match(
            index="runbooks",
            query_text=query_text,
            fields=["title^2", "body"],
            size=top_k,
        )

    async def search_resolved_incidents(
        self, query_text: str, top_k: int = 3
    ) -> list[dict[str, Any]]:
        return await self._multi_match(
            index="incidents",
            query_text=query_text,
            fields=["title^2", "description"],
            size=top_k,
            filters={"term": {"status": "RESOLVED"}},
        )

    async def _multi_match(
        self,
        index: str,
        query_text: str,
        fields: list[str],
        size: int,
        filters: dict[str, Any] | None = None,
    ) -> list[dict[str, Any]]:
        try:
            query: dict[str, Any] = {
                "multi_match": {
                    "query": query_text,
                    "fields": fields,
                    "type": "best_fields",
                }
            }

            if filters:
                query = {
                    "bool": {
                        "must": [query],
                        "filter": [filters],
                    }
                }

            payload = {"query": query, "size": size}

            logger.debug("es_keyword_search_query", index=index, query=query_text[:80])

            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.post(
                    f"{self._base_url}/{index}/_search",
                    json=payload,
                    headers={"Content-Type": "application/json"},
                )
                resp.raise_for_status()
                data = resp.json()

            hits = data.get("hits", {}).get("hits", [])
            top_score = hits[0]["_score"] if hits else None
            logger.info("es_keyword_search_completed", index=index, hit_count=len(hits), top_score=top_score)
            return [
                {
                    "_id": h["_id"],
                    "_score": h["_score"],
                    **h["_source"],
                }
                for h in hits
            ]
        except Exception as e:
            logger.error(
                "es_search_failed", index=index, error=str(e),
            )
            return []


es_client = ElasticsearchClient()
