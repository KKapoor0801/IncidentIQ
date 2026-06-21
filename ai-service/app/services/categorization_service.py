import structlog

from app.clients.ollama_client import ollama_client

logger = structlog.get_logger()

VALID_CATEGORIES = {
    "PAYMENTS", "AUTH", "INFRA", "DATABASE", "NETWORK", "UNKNOWN",
}

SYSTEM_PROMPT = (
    "You are an incident categorization engine. "
    "Given an incident title and description, "
    "classify it into exactly one category. "
    'Respond with JSON only: {"category": "<CATEGORY>"}\n\n'
    "Valid categories: PAYMENTS, AUTH, INFRA, DATABASE, NETWORK, UNKNOWN\n\n"
    "Rules:\n"
    "- PAYMENTS: payment processing, checkout, billing, Stripe\n"
    "- AUTH: authentication, authorization, SSO, tokens, login\n"
    "- INFRA: infrastructure, deployment, containers, k8s\n"
    "- DATABASE: database connections, queries, migrations, pools\n"
    "- NETWORK: DNS, CDN, connectivity, timeouts, SSL/TLS\n"
    "- UNKNOWN: if none of the above clearly fit"
)


async def categorize(title: str, description: str) -> str:
    prompt = f"Title: {title}\nDescription: {description}"

    result = await ollama_client.generate_json(prompt, SYSTEM_PROMPT)
    category = str(result.get("category", "UNKNOWN")).upper()

    if category not in VALID_CATEGORIES:
        logger.warning("invalid_category_from_llm", raw_category=category)
        category = "UNKNOWN"

    logger.info("categorization_result", category=category)
    return category
