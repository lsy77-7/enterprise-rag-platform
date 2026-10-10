package com.example.rag.repository;

import com.example.rag.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            select m
            from ChatMessage m
            where m.conversation.id = :conversationId
            order by m.createdAt desc, m.id desc
            """)
    List<ChatMessage> findRecentMessages(
            @Param("conversationId") Long conversationId,
            Pageable pageable
    );

    List<ChatMessage> findAllByConversation_IdOrderByCreatedAtAscIdAsc(Long conversationId);
}
