from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field

from week1.day5.resume_parser import Resume


class ResumeRecord(BaseModel):
    id: str
    filename: str
    parsed_at: datetime
    data: Resume


class ResumePage(BaseModel):
    items: list[ResumeRecord]
    total: int = Field(ge=0)
    page: int = Field(ge=1)
    page_size: int = Field(ge=1)


class HealthResponse(BaseModel):
    status: str


class ErrorResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    detail: str
