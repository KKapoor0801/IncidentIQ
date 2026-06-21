from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, Field


class IncidentCreatedEvent(BaseModel):
    model_config = {"populate_by_name": True}

    event_id: UUID = Field(alias="eventId")
    event_type: str = Field(alias="eventType")
    incident_id: UUID = Field(alias="incidentId")
    title: str
    description: str
    reporter_id: UUID = Field(alias="reporterId")
    created_at: datetime = Field(alias="createdAt")
    schema_version: str = Field(alias="schemaVersion")
    trace_id: str | None = Field(default=None, alias="traceId")


class IncidentUpdatedEvent(BaseModel):
    model_config = {"populate_by_name": True}

    event_id: UUID = Field(alias="eventId")
    event_type: str = Field(alias="eventType")
    incident_id: UUID = Field(alias="incidentId")
    changed_fields: list[str] = Field(alias="changedFields")
    title: str
    description: str
    requires_reprocessing: bool = Field(alias="requiresReprocessing")
    updated_at: datetime = Field(alias="updatedAt")
    schema_version: str = Field(alias="schemaVersion")
    trace_id: str | None = Field(default=None, alias="traceId")
