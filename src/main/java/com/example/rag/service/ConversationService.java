package com.example.rag.service;

import com.example.rag.common.exception.ResourceNotFoundException;
import com.example.rag.dto.request.ConversationCreateRequest;
import com.example.rag.dto.request.ConversationRenameRequest;
import com.example.rag.dto.response.ChatMessageResponse;
import com.example.rag.dto.response.ConversationDetailResponse;
import com.example.rag.dto.response.ConversationResponse;
import com.example.rag.entity.ChatMessage;
import com.example.rag.entity.Conversation;
import com.example.rag.entity.KnowledgeBase;
import com.example.rag.repository.ChatMessageRepository;
import com.example.rag.repository.ConversationRepository;
import com.example.rag.repository.KnowledgeBaseRepository;
import com.example.rag.repository.UserKnowledgeBaseRepository;
import com.example.rag.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ConversationService {

    private static final int MAX_HISTORY_MESSAGES = 6;
    public static final String DEFAULT_CONVERSATION_TITLE = "新对话";

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final UserKnowledgeBaseRepository userKnowledgeBaseRepository;
    private final PermissionService permissionService;
    private final CurrentUserService currentUserService;
    private final ConversationTitleGenerator conversationTitleGenerator;

    public ConversationService(
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            UserKnowledgeBaseRepository userKnowledgeBaseRepository,
            PermissionService permissionService,
            CurrentUserService currentUserService,
            ConversationTitleGenerator conversationTitleGenerator
    ) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.userKnowledgeBaseRepository = userKnowledgeBaseRepository;
        this.permissionService = permissionService;
        this.currentUserService = currentUserService;
        this.conversationTitleGenerator = conversationTitleGenerator;
    }

    @Transactional
    public ConversationResponse create(ConversationCreateRequest request) {
        permissionService.assertCanAccessKnowledgeBase(
                currentUserService.requireUserId(),
                request.getKnowledgeBaseId()
        );
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(request.getKnowledgeBaseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "知识库不存在: " + request.getKnowledgeBaseId()
                ));

        Conversation conversation = new Conversation();
        conversation.setKnowledgeBase(knowledgeBase);
        conversation.setUserId(currentUserService.requireUserId());
        conversation.setTitle(StringUtils.hasText(request.getTitle())
                ? request.getTitle()
                : DEFAULT_CONVERSATION_TITLE);
        conversation.setManualRenamed(false);

        return ConversationResponse.from(conversationRepository.save(conversation));
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list() {
        CurrentUser currentUser = currentUserService.requireUser();
        List<Conversation> conversations = conversationRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(conversation -> currentUser.id().equals(conversation.getUserId()))
                .toList();

        return conversations.stream()
                .map(ConversationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse getById(Long id) {
        return ConversationResponse.from(getConversation(id));
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getDetail(Long id) {
        Conversation conversation = getConversation(id);
        assertOwner(conversation);
        List<ChatMessageResponse> messages = chatMessageRepository
                .findAllByConversation_IdOrderByCreatedAtAscIdAsc(id)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();

        return ConversationDetailResponse.of(conversation, messages);
    }

    @Transactional
    public void delete(Long id) {
        Conversation conversation = getConversation(id);
        assertOwner(conversation);
        conversationRepository.delete(conversation);
    }

    @Transactional
    public ConversationResponse rename(Long id, ConversationRenameRequest request) {
        Conversation conversation = getConversation(id);
        assertOwner(conversation);

        String title = request.getTitle() == null ? "" : request.getTitle().strip();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (title.length() > 50) {
            throw new IllegalArgumentException("标题不能超过 50 个字符");
        }

        conversation.setTitle(title);
        conversation.setManualRenamed(true);
        return ConversationResponse.from(conversationRepository.save(conversation));
    }

    @Transactional
    public void generateTitleIfNeeded(Conversation conversation, String firstQuestion) {
        if (conversation.isManualRenamed()
                || !DEFAULT_CONVERSATION_TITLE.equals(conversation.getTitle())) {
            return;
        }

        String generatedTitle = conversationTitleGenerator.generate(firstQuestion);
        if (StringUtils.hasText(generatedTitle)) {
            conversation.setTitle(generatedTitle);
            conversation.setManualRenamed(false);
            conversationRepository.save(conversation);
        }
    }

    @Transactional
    public void saveMessage(Conversation conversation, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content);
        chatMessageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getRecentMessages(Long conversationId) {
        List<ChatMessage> recent = chatMessageRepository.findRecentMessages(
                conversationId,
                PageRequest.of(0, MAX_HISTORY_MESSAGES)
        );

        List<ChatMessage> ordered = new ArrayList<>(recent);
        Collections.reverse(ordered);
        return ordered;
    }

    @Transactional(readOnly = true)
    public Conversation getConversation(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("会话不存在: " + id));
    }

    private void assertOwner(Conversation conversation) {
        if (!currentUserService.requireUserId().equals(conversation.getUserId())) {
            throw new ResourceNotFoundException("会话不存在: " + conversation.getId());
        }
    }
}
