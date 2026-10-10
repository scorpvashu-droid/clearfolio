import os
import re
import uuid
from io import BytesIO
from zipfile import BadZipFile, ZipFile

import pymupdf
from docx.opc.exceptions import PackageNotFoundError
from fastapi import APIRouter, File, HTTPException, Query, UploadFile, status
from lxml.etree import XMLSyntaxError

from backend.app import database
from backend.app.schemas import HealthResponse, ResumePage, ResumeRecord
from week1.day5.resume_parser import OCRRequiredError, ResumeTextError, parse_resume_file


router = APIRouter(prefix="/api")
_max_upload_mb = int(os.getenv("MAX_UPLOAD_MB", "10"))
if not 1 <= _max_upload_mb <= 100:
    raise ValueError("MAX_UPLOAD_MB must be between 1 and 100.")
MAX_UPLOAD_BYTES = _max_upload_mb * 1024 * 1024
_ALLOWED_MIME = {
    ".pdf": {"application/pdf", "application/octet-stream"},
    ".docx": {
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/octet-stream",
    },
}


@router.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok")


@router.post(
    "/resumes/parse",
    response_model=ResumeRecord,
    status_code=status.HTTP_201_CREATED,
    responses={400: {"description": "Invalid upload"}, 413: {"description": "File is too large"}},
)
async def parse_uploaded_resume(file: UploadFile = File(...)) -> ResumeRecord:
    original_name = file.filename or ""
    safe_filename = re.sub(r"[\x00-\x1f\x7f]", "", re.split(r"[/\\]", original_name)[-1]).strip()[:255]
    suffix = "." + safe_filename.rsplit(".", 1)[-1].lower() if "." in safe_filename else ""
    if suffix not in _ALLOWED_MIME:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Upload a PDF or DOCX resume.")
    if file.content_type not in _ALLOWED_MIME[suffix]:
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            "The uploaded file's MIME type does not match its extension.",
        )

    content = await file.read(MAX_UPLOAD_BYTES + 1)
    if not content:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "The uploaded file is empty.")
    if len(content) > MAX_UPLOAD_BYTES:
        raise HTTPException(
            status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            f"The file exceeds the {_max_upload_mb} MB limit.",
        )

    if suffix == ".pdf":
        if not content.startswith(b"%PDF-"):
            raise HTTPException(status.HTTP_400_BAD_REQUEST, "The file is not a valid PDF document.")
    else:
        try:
            with ZipFile(BytesIO(content)) as archive:
                entries = archive.infolist()
                if (
                    "word/document.xml" not in {entry.filename for entry in entries}
                    or len(entries) > 2_000
                    or sum(entry.file_size for entry in entries) > 50 * 1024 * 1024
                ):
                    raise HTTPException(status.HTTP_400_BAD_REQUEST, "The file is not a valid DOCX document.")
        except BadZipFile as error:
            raise HTTPException(status.HTTP_400_BAD_REQUEST, "The DOCX document is malformed.") from error

    try:
        parsed = parse_resume_file(content, safe_filename)
    except OCRRequiredError as error:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, str(error)) from error
    except (
        ResumeTextError,
        pymupdf.FileDataError,
        PackageNotFoundError,
        BadZipFile,
        XMLSyntaxError,
        KeyError,
        UnicodeDecodeError,
        ValueError,
    ) as error:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY,
            "The document could not be read. Check that it is a valid PDF or DOCX with readable text.",
        ) from error

    return database.save_resume(str(uuid.uuid4()), safe_filename, parsed)


@router.get("/resumes", response_model=ResumePage)
def resume_history(
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=10, ge=1, le=100),
) -> ResumePage:
    items, total = database.list_resumes(page, page_size)
    return ResumePage(items=items, total=total, page=page, page_size=page_size)


@router.get("/resumes/{resume_id}", response_model=ResumeRecord)
def resume_detail(resume_id: str) -> ResumeRecord:
    try:
        uuid.UUID(resume_id)
    except ValueError as error:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Resume not found.") from error
    record = database.get_resume(resume_id)
    if record is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Resume not found.")
    return record


@router.delete("/resumes/{resume_id}", status_code=status.HTTP_204_NO_CONTENT)
def remove_resume(resume_id: str) -> None:
    try:
        uuid.UUID(resume_id)
    except ValueError as error:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Resume not found.") from error
    if not database.delete_resume(resume_id):
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Resume not found.")
