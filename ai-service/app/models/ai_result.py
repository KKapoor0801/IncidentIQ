from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, Field


class AiClassificationResult(BaseModel):
    incident_id: UUID
    category: str
    priority: str
    confidence_score: float = Field(ge=0.0, le=1.0)
    resolution_suggestion: str | None = None
    model_used: str
    processed_at: datetime
