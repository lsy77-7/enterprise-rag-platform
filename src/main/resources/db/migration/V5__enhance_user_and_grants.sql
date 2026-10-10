ALTER TABLE conversation
    ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1 AFTER knowledge_base_id,
    ADD KEY idx_conversation_user_id (user_id),
    ADD CONSTRAINT fk_conversation_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id);

ALTER TABLE user_knowledge_base
    ADD COLUMN permission VARCHAR(20) NOT NULL DEFAULT 'READ' AFTER knowledge_base_id;
