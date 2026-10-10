package com.example.rag.infrastructure.vectorstore;

public record VectorSearchResult(
        long chunkId,
        long documentId,
        long knowledgeBaseId,
        int chunkIndex,
        Integer pageNumber,
        double score,
        String content
) {
}
