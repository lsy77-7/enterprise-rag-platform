package com.example.rag.infrastructure.embedding;

import com.example.rag.common.exception.EmbeddingNotConfiguredException;

import java.util.List;

public class DisabledEmbeddingService implements EmbeddingService {

    @Override
    public List<float[]> embed(List<String> texts) {
        throw new EmbeddingNotConfiguredException("Embedding service is not configured");
    }

    @Override
    public float[] embed(String text) {
        throw new EmbeddingNotConfiguredException("Embedding service is not configured");
    }

    @Override
    public boolean isConfigured() {
        return false;
    }
}
