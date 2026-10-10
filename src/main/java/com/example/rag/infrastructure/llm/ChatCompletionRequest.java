package com.example.rag.infrastructure.llm;

import java.util.List;

public record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        double temperature
) {
}
