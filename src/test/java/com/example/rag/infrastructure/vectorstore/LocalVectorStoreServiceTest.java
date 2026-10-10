package com.example.rag.infrastructure.vectorstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalVectorStoreServiceTest {

    @TempDir(cleanup = CleanupMode.NEVER)
    Path tempDir;

    private LocalVectorStoreService vectorStore;

    @BeforeEach
    void setUp() {
        vectorStore = new LocalVectorStoreService(tempDir.toString(), new ObjectMapper());
    }

    @Test
    void shouldSaveAndSearchVector() {
        vectorStore.save(vector(1L, 1L, 1L, 0, "hello", new float[]{1, 0}));

        List<VectorSearchResult> results = vectorStore.search(new float[]{1, 0}, 1L, 5);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).chunkId());
        assertEquals("hello", results.get(0).content());
    }

    @Test
    void shouldSaveAllWithoutDuplicatingChunks() {
        vectorStore.save(vector(1L, 1L, 1L, 0, "old", new float[]{1, 0}));
        vectorStore.saveAll(List.of(vector(1L, 1L, 1L, 0, "new", new float[]{0, 1})));

        List<VectorSearchResult> results = vectorStore.search(new float[]{0, 1}, 1L, 5);

        assertEquals(1, results.size());
        assertEquals("new", results.get(0).content());
    }

    @Test
    void shouldLimitTopKAndSortByScoreDescending() {
        vectorStore.saveAll(List.of(
                vector(1L, 1L, 1L, 0, "best", new float[]{1, 0}),
                vector(2L, 1L, 1L, 1, "second", new float[]{0.9f, 0.1f}),
                vector(3L, 1L, 1L, 2, "orthogonal", new float[]{0, 1})
        ));

        List<VectorSearchResult> results = vectorStore.search(new float[]{1, 0}, 1L, 2);

        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).chunkId());
        assertEquals(2L, results.get(1).chunkId());
    }

    @Test
    void shouldDeleteByDocumentId() {
        vectorStore.saveAll(List.of(
                vector(1L, 1L, 1L, 0, "doc1", new float[]{1, 0}),
                vector(2L, 2L, 1L, 0, "doc2", new float[]{0, 1})
        ));

        vectorStore.deleteByDocumentId(1L);

        List<VectorSearchResult> results = vectorStore.search(new float[]{1, 0}, 5);
        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).documentId());
    }

    @Test
    void shouldDeleteByKnowledgeBaseId() {
        vectorStore.saveAll(List.of(
                vector(1L, 1L, 1L, 0, "kb1", new float[]{1, 0}),
                vector(2L, 1L, 2L, 0, "kb2", new float[]{0, 1})
        ));

        vectorStore.deleteByKnowledgeBaseId(1L);

        assertTrue(vectorStore.search(new float[]{1, 0}, 1L, 5).isEmpty());
        assertEquals(1, vectorStore.search(new float[]{0, 1}, 5).size());
    }

    @Test
    void shouldNotReturnRecordsFromOtherKnowledgeBase() {
        vectorStore.saveAll(List.of(
                vector(1L, 1L, 1L, 0, "kb1-doc", new float[]{1, 0}),
                vector(2L, 2L, 2L, 0, "kb2-doc", new float[]{1, 0})
        ));

        List<VectorSearchResult> kb1Results = vectorStore.search(new float[]{1, 0}, 1L, 5);
        List<VectorSearchResult> kb2Results = vectorStore.search(new float[]{1, 0}, 2L, 5);

        assertEquals(1, kb1Results.size());
        assertEquals(1L, kb1Results.get(0).knowledgeBaseId());
        assertEquals("kb1-doc", kb1Results.get(0).content());

        assertEquals(1, kb2Results.size());
        assertEquals(2L, kb2Results.get(0).knowledgeBaseId());
        assertEquals("kb2-doc", kb2Results.get(0).content());
    }

    private VectorRecord vector(
            long chunkId,
            long documentId,
            long knowledgeBaseId,
            int chunkIndex,
            String content,
            float[] embedding
    ) {
        return new VectorRecord(
                chunkId,
                documentId,
                knowledgeBaseId,
                chunkIndex,
                1,
                content,
                embedding,
                Map.of()
        );
    }
}
