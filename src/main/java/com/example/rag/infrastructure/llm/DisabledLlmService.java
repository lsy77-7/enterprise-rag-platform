package com.example.rag.infrastructure.llm;

import com.example.rag.common.exception.LlmNotConfiguredException;

public class DisabledLlmService implements LlmService {

    @Override
    public String chat(String prompt) {
        throw new LlmNotConfiguredException("LLM service is not configured");
    }

    @Override
    public boolean isConfigured() {
        return false;
    }
}
