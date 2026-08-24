CREATE TABLE upload_jobs (
    id            UUID PRIMARY KEY,
    status        VARCHAR(20) NOT NULL,
    error_message TEXT,
    submitted_at  TIMESTAMP NOT NULL,
    completed_at  TIMESTAMP
);