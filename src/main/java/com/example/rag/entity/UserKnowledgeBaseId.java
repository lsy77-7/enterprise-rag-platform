package com.example.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class UserKnowledgeBaseId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "knowledge_base_id")
    private Long knowledgeBaseId;
}
