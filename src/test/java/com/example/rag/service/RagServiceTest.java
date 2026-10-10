package com.example.rag.service;

import com.example.rag.dto.request.RagChatRequest;
import com.example.rag.dto.request.RetrievalSearchRequest;
import com.example.rag.dto.response.RagChatResponse;
import com.example.rag.dto.response.RetrievalSearchResponse;
import com.example.rag.infrastructure.llm.LlmService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagServiceTest {

    @Test
    void shouldBuildPromptCallLlmAndReturnSources() {
        RetrievalService retrievalService = mock(RetrievalService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireUserId()).thenReturn(1L);
        RetrievalSearchResponse source = new RetrievalSearchResponse(
                1L,
                1L,
                1L,
                0,
                2,
                0.82,
                "春季可以种植土豆。"
        );
        when(retrievalService.search(any(RetrievalSearchRequest.class)))
                .thenReturn(List.of(source));

        LlmService llmService = new LlmService() {
            @Override
            public String chat(String prompt) {
                return "根据知识库内容，春季可以种植土豆。";
            }

            @Override
            public boolean isConfigured() {
                return true;
            }
        };

        RagService ragService = new RagService(
                retrievalService,
                llmService,
                mock(PermissionService.class),
                currentUserService
        );

        RagChatRequest request = new RagChatRequest();
        request.setKnowledgeBaseId(1L);
        request.setQuestion("春季应该种什么？");
        request.setTopK(3);

        RagChatResponse response = ragService.chat(request);

        assertEquals("根据知识库内容，春季可以种植土豆。", response.getAnswer());
        assertEquals(1, response.getSources().size());
        assertEquals(2, response.getSources().get(0).getPageNumber());
    }
}
