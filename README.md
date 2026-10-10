# Clearfolio Resume Parser

Clearfolio is a local-first resume parser with a React frontend and a Python/FastAPI API. It extracts text from PDF and DOCX files, turns recognizable details into structured fields, and saves parsing history in a local SQLite database.

Uploaded documents are processed in memory and are not retained. Resume content is not sent to external AI services. Parsing is heuristic, so review results before using them. This project does not claim GDPR or other regulatory compliance.

## Repository layout

```text
clearfolio/
├── .github/workflows/ci.yml
├── backend/
│   ├── app/
│   │   ├── api/routes.py
│   │   ├── database.py
│   │   ├── main.py
│   │   └── schemas.py
│   ├── tests/test_api.py
│   ├── .env.example
│   └── requirements.txt
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   └── types/
│   ├── .env.example
│   └── package.json
└── week1/day5/resume_parser.py
```

## Prerequisites

- Python 3.11 or later
- Node.js 20.19+ or 22.12+ and npm (required by Vite 7)

## Setup and run

Run commands from the `clearfolio` directory. Create a Python environment and install the backend:

```powershell
py -3 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r backend\requirements.txt
Copy-Item backend\.env.example backend\.env
```

Start the API in one terminal:

```powershell
python -m backend.app.main
```

Start the frontend in another terminal:

```powershell
Set-Location frontend
npm ci
Copy-Item .env.example .env
npm run dev
```

Open the Vite URL, usually `http://localhost:5173`. Keep both terminals running while using the application.

## Configuration

Backend settings are in `backend/.env`:

| Variable | Default | Description |
| --- | --- | --- |
| `BACKEND_HOST` | `127.0.0.1` | API bind address |
| `BACKEND_PORT` | `8000` | API port |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | The only allowed browser origin |
| `MAX_UPLOAD_MB` | `10` | Maximum upload size; supported range is 1–100 MB |
| `DATABASE_PATH` | `backend/data/resume_history.sqlite3` | SQLite database file |

Frontend settings are in `frontend/.env`:

| Variable | Default | Description |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `http://127.0.0.1:8000/api` | API base URL |
| `VITE_MAX_UPLOAD_MB` | `10` | Client-side limit; keep it in sync with `MAX_UPLOAD_MB` |

The backend creates the database and table on startup. CORS allows only the configured `FRONTEND_ORIGIN`.

## Features

- PDF and DOCX upload with drag-and-drop, MIME/extension/size checks, and upload progress.
- Text extraction from PDF pages and DOCX paragraphs/tables. Uploaded files are not saved to disk.
- Structured contact, summary, technical and soft skills, education, work experience, projects, certifications, and language fields.
- Conservative extraction: unavailable fields remain null or empty instead of being fabricated.
- Scanned image-only PDFs return an OCR-required message; this app does not run OCR.
- Review, edit, copy, and download parsed JSON; download a text report.
- Paginated SQLite history with open and delete actions.

History contains the parsed result, sanitized original filename, and timestamp. Delete records in the History page or remove the local database to clear it.

## Tests and production build

From the repository root:

```powershell
python -m pytest backend\tests -q
Set-Location frontend
npm ci
npm run build
npm audit
```

The backend tests create temporary documents and an isolated temporary database.

## API

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/health` | API status |
| `POST` | `/api/resumes/parse` | Parse a multipart field named `file` |
| `GET` | `/api/resumes?page=1&page_size=10` | Paginated history |
| `GET` | `/api/resumes/{resume_id}` | Saved parse result |
| `DELETE` | `/api/resumes/{resume_id}` | Delete a history record (`204`) |

Example (PowerShell):

```powershell
curl.exe -X POST http://127.0.0.1:8000/api/resumes/parse `
  -F "file=@C:\path\to\resume.pdf;type=application/pdf"
```

The parse endpoint returns `201` with this structure:

```json
{
  "id": "generated UUID",
  "filename": "resume.pdf",
  "parsed_at": "2026-10-10T12:00:00Z",
  "data": {
    "name": "Jordan Lee",
    "email": "jordan@example.com",
    "phone": null,
    "location": null,
    "summary": null,
    "total_experience_years": null,
    "skills": ["Python"],
    "technical_skills": ["Python"],
    "soft_skills": [],
    "education": [],
    "experiences": [],
    "projects": [],
    "certifications": [],
    "languages": []
  }
}
```

Errors use a JSON `detail` message. Invalid or malformed files return a client error; files above the configured limit return `413`.
