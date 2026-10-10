from io import BytesIO
from zipfile import ZIP_DEFLATED, ZipFile

import pymupdf
import pytest
from docx import Document
from fastapi.testclient import TestClient

from backend.app import database
from backend.app.api import routes
from backend.app.main import app


@pytest.fixture
def client(tmp_path, monkeypatch):
    monkeypatch.setattr(database, "DB_PATH", tmp_path / "test-history.sqlite3")
    with TestClient(app) as test_client:
        yield test_client


def make_pdf(text: str = "") -> bytes:
    document = pymupdf.open()
    page = document.new_page()
    if text:
        page.insert_text((72, 72), text)
    content = document.tobytes()
    document.close()
    return content


def make_docx(text: str) -> bytes:
    document = Document()
    for line in text.splitlines():
        document.add_paragraph(line)
    output = BytesIO()
    document.save(output)
    return output.getvalue()


def make_broken_docx_package() -> bytes:
    output = BytesIO()
    with ZipFile(output, "w", ZIP_DEFLATED) as archive:
        archive.writestr("word/document.xml", "<not-valid-xml")
    return output.getvalue()


def upload(client: TestClient, filename: str, content: bytes, content_type: str):
    return client.post(
        "/api/resumes/parse",
        files={"file": (filename, content, content_type)},
    )


def test_health_and_pdf_upload_are_structured_and_persisted(client):
    assert client.get("/api/health").json() == {"status": "ok"}
    response = upload(
        client,
        "resume.pdf",
        make_pdf(
            "Jordan Lee\njordan@example.com | +1 415 555 0198 | Austin, TX\n"
            "Professional Summary\nBackend engineer with 4 years of experience.\n"
            "Skills\nPython, FastAPI, PostgreSQL, communication\n"
            "Experience\nSoftware Engineer at Example Inc. 2021 - Present\n"
            "Built reliable APIs.\nEducation\nB.S. in Computer Science, State University\n"
            "Projects\nOpen source API\nCertifications\nAWS Certified Developer\n"
            "Languages\nEnglish"
        ),
        "application/pdf",
    )
    assert response.status_code == 201, response.text
    record = response.json()
    parsed = record["data"]
    assert record["filename"] == "resume.pdf"
    assert parsed["name"] == "Jordan Lee"
    assert parsed["email"] == "jordan@example.com"
    assert parsed["phone"] == "+1 415 555 0198"
    assert parsed["summary"].startswith("Backend engineer")
    assert "Python" in parsed["technical_skills"]
    assert parsed["experiences"][0]["job_title"] == "Software Engineer"
    assert parsed["experiences"][0]["company"] == "Example Inc."
    assert parsed["experiences"][0]["end_date"] == "Present"
    assert parsed["projects"] == ["Open source API"]
    assert parsed["certifications"] == ["AWS Certified Developer"]
    assert client.get(f"/api/resumes/{record['id']}").json() == record
    assert client.get("/api/resumes?page=1&page_size=1").json()["total"] == 1


def test_docx_upload_and_missing_fields_are_supported(client):
    response = upload(
        client,
        r"C:\private\sparse.docx",
        make_docx("Taylor\nSkills\nPython"),
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    )
    assert response.status_code == 201, response.text
    assert response.json()["filename"] == "sparse.docx"
    data = response.json()["data"]
    assert data["name"] == "Taylor"
    assert data["email"] is None
    assert data["education"] == []
    assert data["experiences"] == []
    assert data["languages"] == []


def test_rejects_unsupported_extensions_mime_and_empty_uploads(client):
    assert upload(client, "resume.exe", b"content", "application/octet-stream").status_code == 400
    assert upload(client, "resume.pdf", b"%PDF-1.4", "text/plain").status_code == 400
    response = upload(client, "empty.pdf", b"", "application/pdf")
    assert response.status_code == 400
    assert "empty" in response.json()["detail"].lower()


def test_rejects_malformed_documents_and_identifies_scanned_pdf(client):
    assert upload(client, "bad.pdf", b"%PDF-not-a-real-document", "application/pdf").status_code == 422
    response = upload(client, "scanned.pdf", make_pdf(), "application/pdf")
    assert response.status_code == 422
    assert "OCR is required" in response.json()["detail"]
    assert upload(
        client,
        "bad.docx",
        b"not a zip archive",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ).status_code == 400
    broken_package = upload(
        client,
        "broken.docx",
        make_broken_docx_package(),
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    )
    assert broken_package.status_code == 422


def test_enforces_configured_upload_limit(client, monkeypatch):
    monkeypatch.setattr(routes, "MAX_UPLOAD_BYTES", 4)
    response = upload(client, "large.docx", b"12345", "application/octet-stream")
    assert response.status_code == 413


def test_history_pagination_get_and_delete_missing_record(client):
    first = upload(client, "one.docx", make_docx("Alex One"), "application/vnd.openxmlformats-officedocument.wordprocessingml.document").json()
    second = upload(client, "two.docx", make_docx("Alex Two"), "application/vnd.openxmlformats-officedocument.wordprocessingml.document").json()
    page = client.get("/api/resumes?page=1&page_size=1").json()
    assert page["total"] == 2
    assert len(page["items"]) == 1
    assert client.delete(f"/api/resumes/{first['id']}").status_code == 204
    assert client.delete(f"/api/resumes/{first['id']}").status_code == 404
    assert client.get("/api/resumes/not-a-uuid").status_code == 404
    assert client.get(f"/api/resumes/{second['id']}").status_code == 200
    assert client.get("/api/resumes?page=0").status_code == 422
