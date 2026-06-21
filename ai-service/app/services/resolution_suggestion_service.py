import structlog

from app.clients.elasticsearch_client import es_client
from app.clients.ollama_client import ollama_client

logger = structlog.get_logger()

SYSTEM_PROMPT = (
    "You are an incident resolution assistant. "
    "Given an incident description and optionally a matched runbook, "
    "suggest concrete resolution steps. "
    "Be specific and actionable. Respond in plain text, not JSON."
)


async def suggest_resolution(title: str, description: str) -> str | None:
    logger.debug("starting_resolution_suggestion", title=title[:50])
    runbook_hits = await es_client.search_runbooks(
        f"{title} {description}", top_k=3,
    )

    resolved_hits = await es_client.search_resolved_incidents(
        f"{title} {description}", top_k=2,
    )

    context_parts: list[str] = []

    for hit in runbook_hits:
        rb_title = hit.get("title", "")
        rb_body = hit.get("body", "")
        context_parts.append(f"Runbook: {rb_title}\n{rb_body}")
        logger.info(
            "runbook_match",
            runbook_id=hit.get("_id"),
            title=rb_title,
            score=hit.get("_score"),
        )

    for hit in resolved_hits:
        inc_title = hit.get("title", "")
        inc_desc = hit.get("description", "")
        context_parts.append(
            f"Previously resolved incident: {inc_title}\n{inc_desc}"
        )

    context_block = "\n\n---\n\n".join(context_parts) if context_parts else ""

    has_context = bool(context_parts)

    if has_context:
        prompt = (
            f"Incident title: {title}\n"
            f"Incident description: {description}\n\n"
            f"Matched context:\n{context_block}\n\n"
            "Based on the incident and the matched context above, "
            "suggest a resolution."
        )
    else:
        prompt = (
            f"Incident title: {title}\n"
            f"Incident description: {description}\n\n"
            "No matching runbooks or past incidents were found. "
            "Based on the incident details alone, suggest a resolution."
        )

    try:
        raw = await ollama_client.generate(prompt, SYSTEM_PROMPT)
        suggestion = raw.strip()
        if suggestion:
            if not has_context:
                suggestion = (
                    "[Note: This is a generic AI-generated suggestion — "
                    "no matching runbook or past incident was found.]\n\n"
                    + suggestion
                )
            logger.info(
                "resolution_suggestion_generated",
                has_context=has_context,
                context_count=len(context_parts),
            )
            return suggestion
    except Exception as e:
        logger.error("resolution_suggestion_failed", error=str(e))

    return None
