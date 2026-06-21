import structlog

from app.clients.ollama_client import ollama_client

logger = structlog.get_logger()

VALID_PRIORITIES = {"P1", "P2", "P3", "P4"}

SYSTEM_PROMPT = (
    "You are an incident prioritization engine. "
    "Given an incident title, description, and category, "
    "assess the severity and assign a priority level. "
    "Respond with JSON only: "
    '{"priority": "<P1|P2|P3|P4>", "confidence": <0.0-1.0>}\n\n'
    "Priority levels:\n"
    "- P1: Critical — complete outage, data loss, security breach\n"
    "- P2: High — major feature degraded, significant user impact\n"
    "- P3: Medium — minor feature issue, workaround available\n"
    "- P4: Low — cosmetic issues, minor bugs, no immediate impact\n\n"
    "Confidence: 0.0 = guessing, 1.0 = very confident."
)


async def prioritize(
    title: str, description: str, category: str
) -> tuple[str, float]:
    prompt = (
        f"Title: {title}\n"
        f"Description: {description}\n"
        f"Category: {category}"
    )

    result = await ollama_client.generate_json(prompt, SYSTEM_PROMPT)

    priority = str(result.get("priority", "P3")).upper()
    if priority not in VALID_PRIORITIES:
        logger.warning("invalid_priority_from_llm", raw_priority=priority)
        priority = "P3"

    try:
        confidence = float(result.get("confidence", 0.5))
        confidence = max(0.0, min(1.0, confidence))
    except (TypeError, ValueError):
        confidence = 0.5

    logger.info(
        "prioritization_result", priority=priority, confidence=confidence,
    )
    return priority, confidence
