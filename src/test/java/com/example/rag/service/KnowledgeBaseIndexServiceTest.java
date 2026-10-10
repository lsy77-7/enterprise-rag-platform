package com.example.rag.service;

import com.example.rag.entity.Document;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.entity.KnowledgeBase;
import com.example.rag.infrastructure.embedding.EmbeddingService;
import com.example.rag.infrastructure.vectorstore.VectorRecord;
import com.example.rag.infrastructure.vectorstore.VectorStoreService;
import com.example.rag.repository.DocumentChunkRepository;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeBaseIndexServiceTest {

    private DocumentRepository documentRepository;
    private DocumentChunkRepository documentChunkRepository;
    private EmbeddingService embeddingService;
    private VectorStoreService vectorStoreService;
    private KnowledgeBaseIndexService indexService;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        documentChunkRepository = mock(DocumentChunkRepository.class);
        embeddingService = mock(EmbeddingService.class);
        vectorStoreService = mock(VectorStoreService.class);

        indexService = new KnowledgeBaseIndexService(
                mock(KnowledgeBaseRepository.class),
                documentChunkRepository,
                documentRepository,
                embeddingService,
                vectorStoreService
        );
    }

    @Test
    void shouldMarkDocumentCompletedAfterSuccessfulIndexing() {
        Document document = document();
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(documentChunkRepository.findByDocument_IdOrderByChunkIndexAsc(1L))
                .thenReturn(List.of(chunk(document)));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(embeddingService.embed(anyList()))
                .thenReturn(List.of(new float[]{1.0f, 0.0f}));

        indexService.indexDocument(1L);

        assertEquals("COMPLETED", document.getStatus());
        assertEquals(null, document.getErrorMessage());
        verify(vectorStoreService).saveAll(anyList());
    }

    @Test
    void shouldMarkDocumentFailedWhenEmbeddingFails() {
        Document document = document();
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(documentChunkRepository.findByDocument_IdOrderByChunkIndexAsc(1L))
                .thenReturn(List.of(chunk(document)));
        when(embeddingService.isConfigured()).thenReturn(true);
        when(embeddingService.embed(anyList()))
                .thenThrow(new RuntimeException("embedding failed"));

        indexService.indexDocument(1L);

        assertEquals("FAILED", document.getStatus());
        assertNotNull(document.getErrorMessage());
        verify(vectorStoreService).deleteByDocumentId(1L);
    }

    private Document document() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(3L);

        Document document = new Document();
        document.setId(1L);
        document.setKnowledgeBase(knowledgeBase);
        document.setOriginalFileName("test.pdf");
        document.setStatus("INDEXING");
        return document;
    }

    private DocumentChunk chunk(Document document) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(11L);
        chunk.setDocument(document);
        chunk.setChunkIndex(0);
        chunk.setPageNumber(1);
        chunk.setContent("测试内容");
        return chunk;
    }
}
