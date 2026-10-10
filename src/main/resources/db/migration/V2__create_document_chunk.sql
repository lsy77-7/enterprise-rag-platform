CREATE TABLE IF NOT EXISTS document_chunk (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content LONGTEXT NOT NULL,
    character_count INT NOT NULL DEFAULT 0,
    page_number INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_document_chunk_document_id (document_id),
    KEY idx_document_chunk_document_chunk_index (document_id, chunk_index),
    CONSTRAINT fk_document_chunk_document
        FOREIGN KEY (document_id) REFERENCES document (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
