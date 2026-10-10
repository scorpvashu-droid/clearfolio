# Clearfolio Resume Parser

Clearfolio is a local-first resume parser with a React frontend and a Java/Spring Boot API. It extracts text from PDF and DOCX files, turns recognizable details into structured fields, and saves parsing history in a local SQLite database.

Uploaded documents are processed in memory and are not retained. Resume content is not sent to external AI services. Parsing is heuristic, so review results before using them. This project does not claim GDPR or other regulatory compliance.

## Repository layout

```text
clearfolio/
├── .github/workflows/ci.yml
├── backend/
│   ├── src/main/java/com/clearfolio/
│   │   ├── api/
│   │   ├── config/
│   │   ├── model/
│   │   └── service/
│   ├── src/main/resources/
│   ├── src/test/java/com/clearfolio/
│   ├── pom.xml
│   ├── .env.example
│   └── ...
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   └── types/
│   ├── .env.example
│   └── package.json
```

## Prerequisites

- Java 21 or later
- Maven 3.9 or later
- Node.js 20.19+ or 22.12+ and npm (required by Vite 7)

## Setup and run

Run commands from the `clearfolio` directory. Copy the backend environment example:

```powershell
Copy-Item backend\.env.example backend\.env
```

Start the API in one terminal:

```powershell
mvn -f backend\pom.xml spring-boot:run
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
| `DATABASE_PATH` | `backend/data/resume_history.sqlite3` | SQLite database file (resolved from the backend working directory) |

Frontend settings are in `frontend/.env`:

| Variable | Default | Description |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `http://127.0.0.1:8000/api` | API base URL |
| `VITE_MAX_UPLOAD_MB` | `10` | Client-side limit; keep it in sync with `MAX_UPLOAD_MB` |

The backend creates the SQLite database and table on startup. CORS allows only the configured `FRONTEND_ORIGIN`.

## Free deployment (Render)

The repository includes a Render Blueprint (`render.yaml`) for the Java API and React static site.

1. Push the repository to GitHub and create a Blueprint on [Render](https://dashboard.render.com/blueprints) using this repository's `render.yaml`.
2. Review and deploy the two free services. The React app is served as a static site and the Java API runs as a free web service.
3. Check `https://clearfolio-api.onrender.com/api/health`, then open `https://clearfolio.onrender.com`.

Free Render web services spin down after inactivity and can take about a minute to wake. Their filesystem is ephemeral, so SQLite history can be lost after restarts, spin-downs, or redeploys. Do not use this setup for important or durable resume data. Render may suspend free services when included monthly usage limits are reached.

Do not use local `.env` files as production secrets or commit them. `VITE_API_BASE_URL` is a public frontend build setting, not a secret.

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
mvn -f backend\pom.xml verify
Set-Location frontend
npm ci
npm run build
npm audit
```

The Spring integration tests create temporary PDF and DOCX documents and use an isolated SQLite database under `backend/target`.

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
