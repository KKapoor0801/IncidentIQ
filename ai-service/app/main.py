from fastapi import FastAPI

from app.api.v1.health import router as health_router

app = FastAPI(
    title="IncidentIQ AI Service",
    description="AI Intelligence Service for incident classification",
    version="0.1.0",
    docs_url="/docs",
    redoc_url=None,
)

app.include_router(health_router)


@app.get("/health")
async def root_health() -> dict[str, str]:
    return {"status": "healthy", "service": "incidentiq-ai"}
