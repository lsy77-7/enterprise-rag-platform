package com.example.rag.infrastructure.vectorstore;

import java.util.List;

public interface VectorStoreService {

    void save(VectorRecord record);

    void saveAll(List<VectorRecord> records);

    List<VectorSearchResult> search(float[] queryVector, Long knowledgeBaseId, int topK);

    List<VectorSearchResult> search(float[] queryVector, int topK);

    void deleteByDocumentId(Long documentId);

    void deleteByKnowledgeBaseId(Long knowledgeBaseId);
}
