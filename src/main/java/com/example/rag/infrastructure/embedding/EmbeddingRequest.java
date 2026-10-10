package com.example.rag.infrastructure.embedding;

import java.util.List;

public record EmbeddingRequest(String model, List<String> input) {
}
