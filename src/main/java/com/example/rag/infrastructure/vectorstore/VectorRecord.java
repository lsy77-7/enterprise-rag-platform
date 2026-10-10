package com.example.rag.infrastructure.vectorstore;

import java.util.Map;

public record VectorRecord(
        long chunkId,
        long documentId,
        long knowledgeBaseId,
        int chunkIndex,
        Integer pageNumber,
        String content,
        float[] embedding,
        Map<String, Object> metadata
) {
}
