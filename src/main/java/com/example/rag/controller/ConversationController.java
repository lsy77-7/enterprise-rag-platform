package com.example.rag.controller;

import com.example.rag.common.ApiResponse;
import com.example.rag.dto.request.ConversationChatRequest;
import com.example.rag.dto.request.ConversationCreateRequest;
import com.example.rag.dto.request.ConversationRenameRequest;
import com.example.rag.dto.response.ConversationDetailResponse;
import com.example.rag.dto.response.ConversationResponse;
import com.example.rag.dto.response.RagChatResponse;
import com.example.rag.service.ConversationChatService;
import com.example.rag.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Conversation", description = "多轮 RAG 会话")
@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationChatService conversationChatService;

    public ConversationController(
            ConversationService conversationService,
            ConversationChatService conversationChatService
    ) {
        this.conversationService = conversationService;
        this.conversationChatService = conversationChatService;
    }

    @Operation(summary = "创建会话")
    @PostMapping
    public ApiResponse<ConversationResponse> create(
            @Valid @RequestBody ConversationCreateRequest request
    ) {
        return ApiResponse.success(conversationService.create(request));
    }

    @Operation(summary = "查询会话列表")
    @GetMapping
    public ApiResponse<List<ConversationResponse>> list() {
        return ApiResponse.success(conversationService.list());
    }

    @Operation(summary = "查询会话详情")
    @GetMapping("/{id}")
    public ApiResponse<ConversationDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.success(conversationService.getDetail(id));
    }

    @Operation(summary = "重命名会话")
    @PatchMapping("/{id}")
    public ApiResponse<ConversationResponse> rename(
            @PathVariable Long id,
            @Valid @RequestBody ConversationRenameRequest request
    ) {
        return ApiResponse.success(conversationService.rename(id, request));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        conversationService.delete(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "多轮 RAG 问答")
    @PostMapping("/{id}/chat")
    public ApiResponse<RagChatResponse> chat(
            @PathVariable Long id,
            @Valid @RequestBody ConversationChatRequest request
    ) {
        return ApiResponse.success(conversationChatService.chat(id, request));
    }
}
