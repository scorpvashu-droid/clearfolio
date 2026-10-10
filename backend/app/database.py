import json
import os
import sqlite3
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from backend.app.schemas import ResumeRecord
from week1.day5.resume_parser import Resume


DB_PATH = Path(os.getenv(
    "DATABASE_PATH",
    str(Path(__file__).resolve().parents[1] / "data" / "resume_history.sqlite3"),
))


def _connect() -> sqlite3.Connection:
    connection = sqlite3.connect(DB_PATH, timeout=10)
    connection.row_factory = sqlite3.Row
    return connection


def init_db() -> None:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    with _connect() as connection:
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS resumes (
                id TEXT PRIMARY KEY,
                filename TEXT NOT NULL,
                parsed_at TEXT NOT NULL,
                data_json TEXT NOT NULL
            )
            """
        )


def save_resume(resume_id: str, filename: str, data: Resume) -> ResumeRecord:
    parsed_at = datetime.now(timezone.utc)
    with _connect() as connection:
        connection.execute(
            "INSERT INTO resumes (id, filename, parsed_at, data_json) VALUES (?, ?, ?, ?)",
            (resume_id, filename, parsed_at.isoformat(), data.model_dump_json()),
        )
    return ResumeRecord(id=resume_id, filename=filename, parsed_at=parsed_at, data=data)


def list_resumes(page: int, page_size: int) -> tuple[list[ResumeRecord], int]:
    offset = (page - 1) * page_size
    with _connect() as connection:
        total = int(connection.execute("SELECT COUNT(*) FROM resumes").fetchone()[0])
        rows = connection.execute(
            "SELECT id, filename, parsed_at, data_json FROM resumes "
            "ORDER BY parsed_at DESC, id DESC LIMIT ? OFFSET ?",
            (page_size, offset),
        ).fetchall()
    return [_record_from_row(row) for row in rows], total


def get_resume(resume_id: str) -> ResumeRecord | None:
    with _connect() as connection:
        row = connection.execute(
            "SELECT id, filename, parsed_at, data_json FROM resumes WHERE id = ?",
            (resume_id,),
        ).fetchone()
    return _record_from_row(row) if row else None


def delete_resume(resume_id: str) -> bool:
    with _connect() as connection:
        cursor = connection.execute("DELETE FROM resumes WHERE id = ?", (resume_id,))
        return cursor.rowcount > 0


def _record_from_row(row: sqlite3.Row) -> ResumeRecord:
    parsed_data: dict[str, Any] = json.loads(row["data_json"])
    return ResumeRecord(
        id=row["id"],
        filename=row["filename"],
        parsed_at=row["parsed_at"],
        data=Resume.model_validate(parsed_data),
    )
