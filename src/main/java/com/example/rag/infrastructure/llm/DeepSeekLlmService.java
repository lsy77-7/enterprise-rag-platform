package com.example.rag.infrastructure.llm;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

public class DeepSeekLlmService implements LlmService {

    private final LlmProperties properties;
    private final RestClient restClient;

    public DeepSeekLlmService(LlmProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String chat(String prompt) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.getModel(),
                List.of(new ChatMessage("user", prompt)),
                0.2
        );

        ChatCompletionResponse response = restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(ChatCompletionResponse.class);

        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()
                || response.choices().get(0).message() == null) {
            throw new IllegalStateException("LLM 服务返回了空响应");
        }

        return response.choices().get(0).message().content();
    }

    @Override
    public boolean isConfigured() {
        return true;
    }
}
