package com.example.rag.infrastructure.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmbeddingResponse(List<EmbeddingData> data, String model, EmbeddingUsage usage) {
}
