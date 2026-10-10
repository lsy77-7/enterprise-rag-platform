package com.example.rag.service;

import com.example.rag.common.exception.EmbeddingNotConfiguredException;
import com.example.rag.common.exception.ResourceNotFoundException;
import com.example.rag.dto.response.ReindexResponse;
import com.example.rag.entity.Document;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.infrastructure.embedding.EmbeddingService;
import com.example.rag.infrastructure.vectorstore.VectorRecord;
import com.example.rag.infrastructure.vectorstore.VectorStoreService;
import com.example.rag.repository.DocumentChunkRepository;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.repository.KnowledgeBaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeBaseIndexService {

    private static final int EMBEDDING_BATCH_SIZE = 30;
    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseIndexService.class);

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public KnowledgeBaseIndexService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            DocumentChunkRepository documentChunkRepository,
            DocumentRepository documentRepository,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService
    ) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    public ReindexResponse reindex(Long knowledgeBaseId) {
        if (!knowledgeBaseRepository.existsById(knowledgeBaseId)) {
            throw new ResourceNotFoundException("知识库不存在: " + knowledgeBaseId);
        }
        if (!embeddingService.isConfigured()) {
            throw new EmbeddingNotConfiguredException("Embedding service is not configured");
        }

        List<DocumentChunk> chunks =
                documentChunkRepository.findByKnowledgeBaseIdOrderByDocumentIdAndChunkIndex(knowledgeBaseId);

        List<String> texts = chunks.stream()
                .map(DocumentChunk::getContent)
                .toList();

        List<float[]> embeddings = embedInBatches(texts);

        if (embeddings.size() != chunks.size()) {
            throw new IllegalStateException("Embedding 返回数量与 Chunk 数量不一致");
        }

        List<VectorRecord> records = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            records.add(new VectorRecord(
                    chunk.getId(),
                    chunk.getDocument().getId(),
                    knowledgeBaseId,
                    chunk.getChunkIndex(),
                    chunk.getPageNumber(),
                    chunk.getContent(),
                    embeddings.get(i),
                    Map.of()
            ));
        }

        vectorStoreService.deleteByKnowledgeBaseId(knowledgeBaseId);
        vectorStoreService.saveAll(records);
        return new ReindexResponse(knowledgeBaseId, records.size());
    }

    @Transactional
    public void indexDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("文档不存在: " + documentId));

        if (!embeddingService.isConfigured()) {
            markFailed(document, "Embedding service is not configured");
            return;
        }

        try {
            markIndexing(document);

            List<DocumentChunk> chunks =
                    documentChunkRepository.findByDocument_IdOrderByChunkIndexAsc(document.getId());
            if (chunks.isEmpty()) {
                markFailed(document, "文档没有可索引的 Chunk");
                return;
            }

            List<String> texts = chunks.stream()
                    .map(DocumentChunk::getContent)
                    .toList();

            List<float[]> embeddings = embedInBatches(texts);
            if (embeddings.size() != chunks.size()) {
                throw new IllegalStateException("Embedding 返回数量与 Chunk 数量不一致");
            }

            List<VectorRecord> records = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                records.add(new VectorRecord(
                        chunk.getId(),
                        document.getId(),
                        document.getKnowledgeBase().getId(),
                        chunk.getChunkIndex(),
                        chunk.getPageNumber(),
                        chunk.getContent(),
                        embeddings.get(i),
                        Map.of()
                ));
            }

            vectorStoreService.deleteByDocumentId(document.getId());
            vectorStoreService.saveAll(records);
            markCompleted(document);
        } catch (Exception ex) {
            try {
                vectorStoreService.deleteByDocumentId(document.getId());
            } finally {
                markFailed(document, ex.getMessage());
                log.error("文档 {} 向量索引失败", documentId, ex);
            }
        }
    }

    private List<float[]> embedInBatches(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }

        List<float[]> embeddings = new ArrayList<>();
        for (int start = 0; start < texts.size(); start += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(start + EMBEDDING_BATCH_SIZE, texts.size());
            embeddings.addAll(embeddingService.embed(texts.subList(start, end)));
        }
        return embeddings;
    }

    private void markIndexing(Document document) {
        document.setStatus("INDEXING");
        document.setErrorMessage(null);
        documentRepository.save(document);
    }

    private void markCompleted(Document document) {
        document.setStatus("COMPLETED");
        document.setErrorMessage(null);
        documentRepository.save(document);
    }

    private void markFailed(Document document, String message) {
        document.setStatus("FAILED");
        document.setErrorMessage(message);
        documentRepository.save(document);
    }
}
