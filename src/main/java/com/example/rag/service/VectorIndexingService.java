package com.example.rag.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class VectorIndexingService {

    private final KnowledgeBaseIndexService knowledgeBaseIndexService;

    public VectorIndexingService(KnowledgeBaseIndexService knowledgeBaseIndexService) {
        this.knowledgeBaseIndexService = knowledgeBaseIndexService;
    }

    @Async("vectorIndexExecutor")
    public void reindexAsync(Long knowledgeBaseId) {
        knowledgeBaseIndexService.reindex(knowledgeBaseId);
    }

    @Async("vectorIndexExecutor")
    public void indexDocumentAsync(Long documentId) {
        knowledgeBaseIndexService.indexDocument(documentId);
    }
}
