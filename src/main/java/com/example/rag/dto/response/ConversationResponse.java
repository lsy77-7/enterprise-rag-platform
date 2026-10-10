package com.example.rag.dto.response;

import com.example.rag.entity.Conversation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private Long id;
    private Long knowledgeBaseId;
    private String title;
    private boolean manualRenamed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ConversationResponse from(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getKnowledgeBase().getId(),
                conversation.getTitle(),
                conversation.isManualRenamed(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
