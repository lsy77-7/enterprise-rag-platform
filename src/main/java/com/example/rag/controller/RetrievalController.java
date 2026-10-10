package com.example.rag.controller;

import com.example.rag.common.ApiResponse;
import com.example.rag.dto.request.RetrievalSearchRequest;
import com.example.rag.dto.response.RetrievalSearchResponse;
import com.example.rag.service.RetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Retrieval", description = "Top-K 向量检索")
@RestController
@RequestMapping("/api/v1/retrieval")
public class RetrievalController {

    private final RetrievalService retrievalService;

    public RetrievalController(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @Operation(summary = "检索知识库", description = "根据 query embedding 进行 Top-K 相似度检索")
    @PostMapping("/search")
    public ApiResponse<List<RetrievalSearchResponse>> search(
            @Valid @RequestBody RetrievalSearchRequest request
    ) {
        return ApiResponse.success(retrievalService.search(request));
    }
}
