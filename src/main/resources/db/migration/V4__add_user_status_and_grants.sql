ALTER TABLE sys_user
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER role;

CREATE TABLE IF NOT EXISTS user_knowledge_base (
    user_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, knowledge_base_id),
    KEY idx_user_knowledge_base_knowledge_base_id (knowledge_base_id),
    CONSTRAINT fk_user_knowledge_base_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_knowledge_base_knowledge_base
        FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
