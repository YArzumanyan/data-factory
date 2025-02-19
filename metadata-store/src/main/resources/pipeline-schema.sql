CREATE TABLE IF NOT EXISTS pipeline_versions (
    pipeline_id VARCHAR(255) NOT NULL,
    version INT NOT NULL,
    payload CLOB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (pipeline_id, version)
);
