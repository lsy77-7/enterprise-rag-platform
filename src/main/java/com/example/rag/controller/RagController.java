package com.example.rag.controller;

import com.example.rag.common.ApiResponse;
import com.example.rag.dto.request.RagChatRequest;
import com.example.rag.dto.response.RagChatResponse;
import com.example.rag.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "RAG", description = "基础 RAG 问答")
@RestController
@RequestMapping("/api/v1/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @Operation(summary = "RAG 问答")
    @PostMapping("/chat")
    public ApiResponse<RagChatResponse> chat(@Valid @RequestBody RagChatRequest request) {
        return ApiResponse.success(ragService.chat(request));
    }
}
