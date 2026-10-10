package com.example.rag.controller;

import com.example.rag.common.ApiResponse;
import com.example.rag.dto.request.BatchGrantRequest;
import com.example.rag.dto.request.BatchRevokeRequest;
import com.example.rag.dto.request.KnowledgeBaseCreateRequest;
import com.example.rag.dto.response.BatchGrantResult;
import com.example.rag.dto.response.BatchRevokeResult;
import com.example.rag.dto.response.KnowledgeBaseResponse;
import com.example.rag.dto.response.KnowledgeBaseGrantResponse;
import com.example.rag.dto.response.ReindexAcceptedResponse;
import com.example.rag.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping
    public ApiResponse<KnowledgeBaseResponse> create(
            @Valid @RequestBody KnowledgeBaseCreateRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.create(request));
    }

    @GetMapping
    public ApiResponse<List<KnowledgeBaseResponse>> list() {
        return ApiResponse.success(knowledgeBaseService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBaseResponse> get(@PathVariable Long id) {
        return ApiResponse.success(knowledgeBaseService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "重建知识库向量索引")
    @PostMapping("/{kbId}/reindex")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<ReindexAcceptedResponse> reindex(@PathVariable Long kbId) {
        knowledgeBaseService.reindex(kbId);
        return ApiResponse.success(new ReindexAcceptedResponse(
                kbId,
                "向量索引任务已提交，正在后台处理"
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{kbId}/grants/{userId}")
    public ApiResponse<Void> grant(
            @PathVariable Long kbId,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "READ") String permission
    ) {
        knowledgeBaseService.grantAccess(kbId, userId, permission);
        return ApiResponse.success(null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{kbId}/grants/{userId}")
    public ApiResponse<Void> revoke(
            @PathVariable Long kbId,
            @PathVariable Long userId
    ) {
        knowledgeBaseService.revokeAccess(kbId, userId);
        return ApiResponse.success(null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{kbId}/grants")
    public ApiResponse<List<KnowledgeBaseGrantResponse>> listGrants(
            @PathVariable Long kbId
    ) {
        return ApiResponse.success(knowledgeBaseService.listGrants(kbId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{kbId}/permissions/batch")
    public ApiResponse<BatchGrantResult> batchGrant(
            @PathVariable Long kbId,
            @Valid @RequestBody BatchGrantRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.batchGrant(kbId, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{kbId}/permissions/batch")
    public ApiResponse<BatchRevokeResult> batchRevoke(
            @PathVariable Long kbId,
            @Valid @RequestBody BatchRevokeRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.batchRevoke(kbId, request));
    }
}
