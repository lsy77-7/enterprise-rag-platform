package com.example.rag.infrastructure.llm;

public interface LlmService {

    String chat(String prompt);

    boolean isConfigured();
}
