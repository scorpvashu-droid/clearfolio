CREATE TABLE IF NOT EXISTS resumes (
    id TEXT PRIMARY KEY,
    filename TEXT NOT NULL,
    parsed_at TEXT NOT NULL,
    data_json TEXT NOT NULL
);
