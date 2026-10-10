package com.example.rag.service;

import com.example.rag.common.exception.LlmNotConfiguredException;
import com.example.rag.dto.request.RagChatRequest;
import com.example.rag.dto.request.RetrievalSearchRequest;
import com.example.rag.dto.response.RagChatResponse;
import com.example.rag.dto.response.RetrievalSearchResponse;
import com.example.rag.infrastructure.llm.LlmService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final RetrievalService retrievalService;
    private final LlmService llmService;
    private final PermissionService permissionService;
    private final CurrentUserService currentUserService;

    public RagService(
            RetrievalService retrievalService,
            LlmService llmService,
            PermissionService permissionService,
            CurrentUserService currentUserService
    ) {
        this.retrievalService = retrievalService;
        this.llmService = llmService;
        this.permissionService = permissionService;
        this.currentUserService = currentUserService;
    }

    public RagChatResponse chat(RagChatRequest request) {
        permissionService.assertCanAccessKnowledgeBase(
                currentUserService.requireUserId(),
                request.getKnowledgeBaseId()
        );
        if (!llmService.isConfigured()) {
            throw new LlmNotConfiguredException("LLM service is not configured");
        }

        RetrievalSearchRequest retrievalRequest = new RetrievalSearchRequest();
        retrievalRequest.setKnowledgeBaseId(request.getKnowledgeBaseId());
        retrievalRequest.setQuery(request.getQuestion());
        retrievalRequest.setTopK(request.getTopK() == null ? 3 : request.getTopK());

        List<RetrievalSearchResponse> sources = retrievalService.search(retrievalRequest);
        String prompt = buildPrompt(request.getQuestion(), sources);
        String answer = llmService.chat(prompt);

        return new RagChatResponse(answer, sources);
    }

    private String buildPrompt(String question, List<RetrievalSearchResponse> sources) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个知识库问答助手。\n\n");
        prompt.append("请根据下面的知识库内容回答用户问题。\n");
        prompt.append("如果知识库中没有相关信息，请明确说不知道，不要自己编造。\n\n");
        prompt.append("知识库内容：\n");

        for (int i = 0; i < sources.size(); i++) {
            RetrievalSearchResponse source = sources.get(i);
            String pageNumber = source.getPageNumber() == null ? "未知" : source.getPageNumber().toString();
            prompt.append("【来源").append(i + 1).append("】\n");
            prompt.append("第").append(pageNumber).append("页：\n");
            prompt.append(source.getContent()).append("\n\n");
        }

        prompt.append("用户问题：\n");
        prompt.append(question).append("\n\n");
        prompt.append("请直接回答问题。");
        return prompt.toString();
    }
}
