package com.example.rag.dto.response;

import com.example.rag.entity.KnowledgeBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseResponse {

    private Long id;
    private String name;
    private String description;
    private String visibility;
    private Long ownerId;
    private LocalDateTime createdAt;

    public static KnowledgeBaseResponse from(KnowledgeBase knowledgeBase) {
        return new KnowledgeBaseResponse(
                knowledgeBase.getId(),
                knowledgeBase.getName(),
                knowledgeBase.getDescription(),
                knowledgeBase.getVisibility(),
                knowledgeBase.getCreatedBy().getId(),
                knowledgeBase.getCreatedAt()
        );
    }
}
