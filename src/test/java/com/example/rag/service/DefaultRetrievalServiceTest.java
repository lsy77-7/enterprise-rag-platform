package com.example.rag.service;

import com.example.rag.dto.request.RetrievalSearchRequest;
import com.example.rag.dto.response.RetrievalSearchResponse;
import com.example.rag.infrastructure.embedding.EmbeddingService;
import com.example.rag.infrastructure.vectorstore.LocalVectorStoreService;
import com.example.rag.infrastructure.vectorstore.VectorRecord;
import com.example.rag.repository.KnowledgeBaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultRetrievalServiceTest {

    @TempDir(cleanup = CleanupMode.NEVER)
    Path tempDir;

    private DefaultRetrievalService retrievalService;
    private KnowledgeBaseRepository knowledgeBaseRepository;
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        currentUserService = mock(CurrentUserService.class);
        when(knowledgeBaseRepository.existsById(1L)).thenReturn(true);
        when(currentUserService.requireUserId()).thenReturn(1L);

        EmbeddingService embeddingService = new EmbeddingService() {
            @Override
            public List<float[]> embed(List<String> texts) {
                return texts.stream().map(text -> new float[]{1, 0}).toList();
            }

            @Override
            public float[] embed(String text) {
                return new float[]{1, 0};
            }

            @Override
            public boolean isConfigured() {
                return true;
            }
        };

        LocalVectorStoreService vectorStore =
                new LocalVectorStoreService(tempDir.toString(), new ObjectMapper());
        vectorStore.saveAll(List.of(
                vector(1L, 1L, 0, "best", new float[]{1, 0}),
                vector(2L, 1L, 1, "second", new float[]{0.9f, 0.1f}),
                vector(3L, 1L, 2, "other", new float[]{0, 1})
        ));

        retrievalService =
                new DefaultRetrievalService(
                        embeddingService,
                        vectorStore,
                        knowledgeBaseRepository,
                        mock(VectorIndexingService.class),
                        mock(PermissionService.class),
                        currentUserService
                );
    }

    @Test
    void shouldSortBySimilarityAndRespectTopK() {
        RetrievalSearchRequest request = new RetrievalSearchRequest();
        request.setKnowledgeBaseId(1L);
        request.setQuery("test");
        request.setTopK(2);

        List<RetrievalSearchResponse> results = retrievalService.search(request);

        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).getChunkId());
        assertEquals(2L, results.get(1).getChunkId());
    }

    private VectorRecord vector(long chunkId, long knowledgeBaseId, int chunkIndex, String content, float[] embedding) {
        return new VectorRecord(
                chunkId,
                1L,
                knowledgeBaseId,
                chunkIndex,
                1,
                content,
                embedding,
                Map.of()
        );
    }
}
