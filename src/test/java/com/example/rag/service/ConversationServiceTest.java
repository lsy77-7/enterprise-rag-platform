package com.example.rag.service;

import com.example.rag.common.exception.ResourceNotFoundException;
import com.example.rag.dto.request.ConversationCreateRequest;
import com.example.rag.dto.response.ConversationResponse;
import com.example.rag.entity.ChatMessage;
import com.example.rag.entity.Conversation;
import com.example.rag.entity.KnowledgeBase;
import com.example.rag.repository.ChatMessageRepository;
import com.example.rag.repository.ConversationRepository;
import com.example.rag.repository.KnowledgeBaseRepository;
import com.example.rag.repository.UserKnowledgeBaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationServiceTest {

    @Test
    void shouldCreateConversation() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(1L);
        ConversationService service = new ConversationService(
                conversationRepository,
                chatMessageRepository,
                knowledgeBaseRepository,
                mock(UserKnowledgeBaseRepository.class),
                mock(PermissionService.class),
                currentUserService,
                mock(ConversationTitleGenerator.class)
        );

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(1L);

        Conversation saved = new Conversation();
        saved.setId(10L);
        saved.setKnowledgeBase(knowledgeBase);
        saved.setTitle("星露谷攻略咨询");

        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(knowledgeBase));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(saved);

        ConversationCreateRequest request = new ConversationCreateRequest();
        request.setKnowledgeBaseId(1L);
        request.setTitle("星露谷攻略咨询");

        ConversationResponse response = service.create(request);

        assertEquals(10L, response.getId());
        assertEquals(1L, response.getKnowledgeBaseId());
        assertEquals("星露谷攻略咨询", response.getTitle());
    }

    @Test
    void shouldReturnRecentMessagesInChronologicalOrder() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(1L);
        ConversationService service = new ConversationService(
                conversationRepository,
                chatMessageRepository,
                knowledgeBaseRepository,
                mock(UserKnowledgeBaseRepository.class),
                mock(PermissionService.class),
                currentUserService,
                mock(ConversationTitleGenerator.class)
        );

        ChatMessage newer = message("assistant", "b");
        ChatMessage older = message("user", "a");

        when(chatMessageRepository.findRecentMessages(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(newer, older));

        List<ChatMessage> messages = service.getRecentMessages(1L);

        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).getRole());
        assertEquals("assistant", messages.get(1).getRole());
    }

    @Test
    void shouldGenerateTitleOnFirstQuestion() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(1L);
        ConversationTitleGenerator titleGenerator = mock(ConversationTitleGenerator.class);
        ConversationService service = new ConversationService(
                conversationRepository,
                chatMessageRepository,
                knowledgeBaseRepository,
                mock(UserKnowledgeBaseRepository.class),
                mock(PermissionService.class),
                currentUserService,
                titleGenerator
        );

        Conversation conversation = new Conversation();
        conversation.setId(10L);
        conversation.setUserId(1L);
        conversation.setTitle(ConversationService.DEFAULT_CONVERSATION_TITLE);
        conversation.setManualRenamed(false);

        String question = "星露谷新手攻略，开局应该做什么？";
        when(titleGenerator.generate(question)).thenReturn("星露谷新手开局攻略");

        service.generateTitleIfNeeded(conversation, question);

        assertEquals("星露谷新手开局攻略", conversation.getTitle());
        verify(conversationRepository).save(conversation);
    }

    @Test
    void shouldNotOverrideManualRenamedTitle() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        ConversationService service = new ConversationService(
                conversationRepository,
                mock(ChatMessageRepository.class),
                mock(KnowledgeBaseRepository.class),
                mock(UserKnowledgeBaseRepository.class),
                mock(PermissionService.class),
                mock(CurrentUserService.class),
                mock(ConversationTitleGenerator.class)
        );

        Conversation conversation = new Conversation();
        conversation.setId(10L);
        conversation.setUserId(1L);
        conversation.setTitle("用户自定义标题");
        conversation.setManualRenamed(true);

        service.generateTitleIfNeeded(conversation, "任何问题");

        assertEquals("用户自定义标题", conversation.getTitle());
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    void shouldDeleteOwnedConversation() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(1L);
        ConversationService service = new ConversationService(
                conversationRepository,
                mock(ChatMessageRepository.class),
                mock(KnowledgeBaseRepository.class),
                mock(UserKnowledgeBaseRepository.class),
                mock(PermissionService.class),
                currentUserService,
                mock(ConversationTitleGenerator.class)
        );

        Conversation conversation = new Conversation();
        conversation.setId(10L);
        conversation.setUserId(1L);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        service.delete(10L);

        verify(conversationRepository).delete(conversation);
    }

    @Test
    void shouldNotDeleteOtherUsersConversation() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(1L);
        ConversationService service = new ConversationService(
                conversationRepository,
                mock(ChatMessageRepository.class),
                mock(KnowledgeBaseRepository.class),
                mock(UserKnowledgeBaseRepository.class),
                mock(PermissionService.class),
                currentUserService,
                mock(ConversationTitleGenerator.class)
        );

        Conversation conversation = new Conversation();
        conversation.setId(10L);
        conversation.setUserId(2L);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        assertThrows(ResourceNotFoundException.class, () -> service.delete(10L));
        verify(conversationRepository, never()).delete(any(Conversation.class));
    }

    private ChatMessage message(String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setRole(role);
        message.setContent(content);
        return message;
    }
}
