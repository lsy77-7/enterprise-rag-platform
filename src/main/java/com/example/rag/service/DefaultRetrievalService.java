package com.example.rag.service;

import com.example.rag.common.exception.EmbeddingNotConfiguredException;
import com.example.rag.common.exception.ResourceNotFoundException;
import com.example.rag.dto.request.RetrievalSearchRequest;
import com.example.rag.dto.response.RetrievalSearchResponse;
import com.example.rag.infrastructure.embedding.EmbeddingService;
import com.example.rag.infrastructure.vectorstore.VectorSearchResult;
import com.example.rag.infrastructure.vectorstore.VectorStoreService;
import com.example.rag.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DefaultRetrievalService implements RetrievalService {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final VectorIndexingService vectorIndexingService;
    private final Set<Long> indexedKnowledgeBaseIds = ConcurrentHashMap.newKeySet();
    private final PermissionService permissionService;
    private final CurrentUserService currentUserService;

    public DefaultRetrievalService(
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            KnowledgeBaseRepository knowledgeBaseRepository,
            VectorIndexingService vectorIndexingService,
            PermissionService permissionService,
            CurrentUserService currentUserService
    ) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.vectorIndexingService = vectorIndexingService;
        this.permissionService = permissionService;
        this.currentUserService = currentUserService;
    }

    @Override
    public List<RetrievalSearchResponse> search(RetrievalSearchRequest request) {
        if (!knowledgeBaseRepository.existsById(request.getKnowledgeBaseId())) {
            throw new ResourceNotFoundException("知识库不存在: " + request.getKnowledgeBaseId());
        }
        permissionService.assertCanAccessKnowledgeBase(
                currentUserService.requireUserId(),
                request.getKnowledgeBaseId()
        );
        if (!embeddingService.isConfigured()) {
            throw new EmbeddingNotConfiguredException("Embedding service is not configured");
        }

        int topK = request.getTopK() == null ? 5 : request.getTopK();
        float[] queryVector = embeddingService.embed(request.getQuery());
        List<VectorSearchResult> results =
                vectorStoreService.search(queryVector, request.getKnowledgeBaseId(), topK);

        if (results.isEmpty() && indexedKnowledgeBaseIds.add(request.getKnowledgeBaseId())) {
            vectorIndexingService.reindexAsync(request.getKnowledgeBaseId());
        }

        return results.stream()
                .map(this::toResponse)
                .toList();
    }

    private RetrievalSearchResponse toResponse(VectorSearchResult result) {
        return new RetrievalSearchResponse(
                result.chunkId(),
                result.documentId(),
                result.knowledgeBaseId(),
                result.chunkIndex(),
                result.pageNumber(),
                Math.round(result.score() * 10000.0) / 10000.0,
                result.content()
        );
    }
}
