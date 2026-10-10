package com.example.rag.controller;

import com.example.rag.common.ApiResponse;
import com.example.rag.dto.response.DocumentReindexAcceptedResponse;
import com.example.rag.dto.response.DocumentResponse;
import com.example.rag.dto.response.DocumentUploadResponse;
import com.example.rag.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge-bases/{kbId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "上传 PDF 文档")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentUploadResponse> upload(
            @PathVariable Long kbId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(documentService.upload(kbId, file));
    }

    @Operation(summary = "查询知识库文档列表")
    @GetMapping
    public ApiResponse<List<DocumentResponse>> list(@PathVariable Long kbId) {
        return ApiResponse.success(documentService.list(kbId));
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/{documentId}")
    public ApiResponse<Void> delete(
            @PathVariable Long kbId,
            @PathVariable Long documentId
    ) {
        documentService.delete(kbId, documentId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "重新索引文档")
    @PostMapping("/{documentId}/reindex")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<DocumentReindexAcceptedResponse> reindex(
            @PathVariable Long kbId,
            @PathVariable Long documentId
    ) {
        documentService.reindexDocument(kbId, documentId);
        return ApiResponse.success(new DocumentReindexAcceptedResponse(
                documentId,
                "文档重新索引任务已提交，正在后台处理"
        ));
    }
}
