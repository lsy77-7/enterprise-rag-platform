ALTER TABLE knowledge_base
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' AFTER description;
