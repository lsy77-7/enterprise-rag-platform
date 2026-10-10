package com.example.rag.service;

import com.example.rag.dto.request.ConversationChatRequest;
import com.example.rag.dto.request.RetrievalSearchRequest;
import com.example.rag.dto.response.RagChatResponse;
import com.example.rag.dto.response.RetrievalSearchResponse;
import com.example.rag.entity.ChatMessage;
import com.example.rag.entity.Conversation;
import com.example.rag.entity.KnowledgeBase;
import com.example.rag.infrastructure.llm.LlmService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationChatServiceTest {

    @Test
    void shouldUseHistoryRetrievalAndSaveMessages() {
        ConversationService conversationService = mock(ConversationService.class);
        RetrievalService retrievalService = mock(RetrievalService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(1L);
        LlmService llmService = new LlmService() {
            @Override
            public String chat(String prompt) {
                return "春季可以种植土豆。";
            }

            @Override
            public boolean isConfigured() {
                return true;
            }
        };

        ConversationChatService service =
                new ConversationChatService(
                        conversationService,
                        retrievalService,
                        llmService,
                        mock(PermissionService.class),
                        currentUserService
                );

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(1L);

        Conversation conversation = new Conversation();
        conversation.setId(7L);
        conversation.setKnowledgeBase(knowledgeBase);
        conversation.setUserId(1L);

        ChatMessage history = new ChatMessage();
        history.setRole("user");
        history.setContent("星露谷春季种什么？");

        RetrievalSearchResponse source = new RetrievalSearchResponse(
                1L,
                1L,
                1L,
                0,
                2,
                0.82,
                "春季可以种植土豆。"
        );

        when(conversationService.getConversation(7L)).thenReturn(conversation);
        when(conversationService.getRecentMessages(7L)).thenReturn(List.of(history));
        when(retrievalService.search(any(RetrievalSearchRequest.class))).thenReturn(List.of(source));

        ConversationChatRequest request = new ConversationChatRequest();
        request.setQuestion("春季适合种什么？");
        request.setTopK(3);

        RagChatResponse response = service.chat(7L, request);

        assertEquals("春季可以种植土豆。", response.getAnswer());
        assertEquals(1, response.getSources().size());
        verify(conversationService).saveMessage(conversation, "user", "春季适合种什么？");
        verify(conversationService).saveMessage(conversation, "assistant", "春季可以种植土豆。");
    }

    @Test
    void shouldTriggerAutoTitleOnFirstQuestion() {
        ConversationService conversationService = mock(ConversationService.class);
        RetrievalService retrievalService = mock(RetrievalService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(1L);
        LlmService llmService = new LlmService() {
            @Override
            public String chat(String prompt) {
                return "春季可以种植土豆。";
            }

            @Override
            public boolean isConfigured() {
                return true;
            }
        };

        ConversationChatService service =
                new ConversationChatService(
                        conversationService,
                        retrievalService,
                        llmService,
                        mock(PermissionService.class),
                        currentUserService
                );

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(1L);

        Conversation conversation = new Conversation();
        conversation.setId(7L);
        conversation.setKnowledgeBase(knowledgeBase);
        conversation.setUserId(1L);

        when(conversationService.getConversation(7L)).thenReturn(conversation);
        when(conversationService.getRecentMessages(7L)).thenReturn(List.of());
        when(retrievalService.search(any(RetrievalSearchRequest.class))).thenReturn(List.of());

        ConversationChatRequest request = new ConversationChatRequest();
        request.setQuestion("春季适合种什么？");
        request.setTopK(3);

        service.chat(7L, request);

        verify(conversationService).generateTitleIfNeeded(conversation, "春季适合种什么？");
    }
}
