package com.example.rag.infrastructure.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmbeddingData(int index, float[] embedding) {
}
