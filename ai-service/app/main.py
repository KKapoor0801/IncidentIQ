import asyncio
import contextlib
from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.v1.health import router as health_router
from app.consumers.incident_event_consumer import start_consumer
from app.core.logging import setup_logging

setup_logging()


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    task = asyncio.create_task(start_consumer())
    yield
    task.cancel()
    with contextlib.suppress(asyncio.CancelledError):
        await task


app = FastAPI(
    title="IncidentIQ AI Service",
    description="AI Intelligence Service for incident classification",
    version="0.1.0",
    docs_url="/docs",
    redoc_url=None,
    lifespan=lifespan,
)

app.include_router(health_router)


@app.get("/health")
async def root_health() -> dict[str, str]:
    return {"status": "healthy", "service": "incidentiq-ai"}
