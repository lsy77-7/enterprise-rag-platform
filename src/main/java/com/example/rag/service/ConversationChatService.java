package com.example.rag.service;

import com.example.rag.common.exception.LlmNotConfiguredException;
import com.example.rag.dto.request.ConversationChatRequest;
import com.example.rag.dto.request.RetrievalSearchRequest;
import com.example.rag.dto.response.RagChatResponse;
import com.example.rag.dto.response.RetrievalSearchResponse;
import com.example.rag.entity.ChatMessage;
import com.example.rag.entity.Conversation;
import com.example.rag.common.exception.ResourceNotFoundException;
import com.example.rag.infrastructure.llm.LlmService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConversationChatService {

    private final ConversationService conversationService;
    private final RetrievalService retrievalService;
    private final LlmService llmService;
    private final PermissionService permissionService;
    private final CurrentUserService currentUserService;

    public ConversationChatService(
            ConversationService conversationService,
            RetrievalService retrievalService,
            LlmService llmService,
            PermissionService permissionService,
            CurrentUserService currentUserService
    ) {
        this.conversationService = conversationService;
        this.retrievalService = retrievalService;
        this.llmService = llmService;
        this.permissionService = permissionService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public RagChatResponse chat(Long conversationId, ConversationChatRequest request) {
        if (!llmService.isConfigured()) {
            throw new LlmNotConfiguredException("LLM service is not configured");
        }

        Conversation conversation = conversationService.getConversation(conversationId);
        if (!currentUserService.requireUserId().equals(conversation.getUserId())) {
            throw new ResourceNotFoundException("会话不存在: " + conversationId);
        }
        Long knowledgeBaseId = conversation.getKnowledgeBase().getId();
        permissionService.assertCanAccessKnowledgeBase(
                currentUserService.requireUserId(),
                knowledgeBaseId
        );
        List<ChatMessage> history = conversationService.getRecentMessages(conversationId);
        boolean firstQuestion = history.isEmpty();

        RetrievalSearchRequest retrievalRequest = new RetrievalSearchRequest();
        retrievalRequest.setKnowledgeBaseId(knowledgeBaseId);
        retrievalRequest.setQuery(request.getQuestion());
        retrievalRequest.setTopK(request.getTopK() == null ? 3 : request.getTopK());

        List<RetrievalSearchResponse> sources = retrievalService.search(retrievalRequest);
        String prompt = buildPrompt(history, request.getQuestion(), sources);
        String answer = llmService.chat(prompt);

        conversationService.saveMessage(conversation, "user", request.getQuestion());
        conversationService.saveMessage(conversation, "assistant", answer);

        if (firstQuestion) {
            conversationService.generateTitleIfNeeded(conversation, request.getQuestion());
        }

        return new RagChatResponse(answer, sources);
    }

    private String buildPrompt(
            List<ChatMessage> history,
            String question,
            List<RetrievalSearchResponse> sources
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个企业知识库问答助手。\n");
        prompt.append("请优先根据提供的知识库内容回答问题。\n");
        prompt.append("如果知识库中没有相关信息，请明确说明，不要编造。\n\n");

        prompt.append("历史对话：\n");
        if (history.isEmpty()) {
            prompt.append("（无）\n");
        } else {
            for (ChatMessage message : history) {
                String roleName = "user".equals(message.getRole()) ? "用户" : "助手";
                prompt.append(roleName).append("：").append(message.getContent()).append("\n");
            }
        }
        prompt.append("\n");

        prompt.append("知识库内容：\n");
        for (int i = 0; i < sources.size(); i++) {
            RetrievalSearchResponse source = sources.get(i);
            String pageNumber = source.getPageNumber() == null ? "未知" : source.getPageNumber().toString();
            prompt.append("【来源").append(i + 1).append("】\n");
            prompt.append("第").append(pageNumber).append("页：\n");
            prompt.append(source.getContent()).append("\n\n");
        }

        prompt.append("当前问题：\n");
        prompt.append(question).append("\n");
        return prompt.toString();
    }
}
