package com.example.rag.dto.response;

import com.example.rag.entity.Conversation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetailResponse {

    private Long id;
    private Long knowledgeBaseId;
    private String title;
    private boolean manualRenamed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChatMessageResponse> messages;

    public static ConversationDetailResponse of(
            Conversation conversation,
            List<ChatMessageResponse> messages
    ) {
        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getKnowledgeBase().getId(),
                conversation.getTitle(),
                conversation.isManualRenamed(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages
        );
    }
}
