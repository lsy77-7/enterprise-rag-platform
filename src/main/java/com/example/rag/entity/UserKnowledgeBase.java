package com.example.rag.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_knowledge_base")
public class UserKnowledgeBase {

    @EmbeddedId
    private UserKnowledgeBaseId id;

    @Column(name = "permission", nullable = false, length = 20)
    private String permission;
}
