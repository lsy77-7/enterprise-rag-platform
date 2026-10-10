package com.example.rag.infrastructure.embedding;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;

public class HttpEmbeddingService implements EmbeddingService {

    private final EmbeddingProperties properties;
    private final RestClient restClient;

    public HttpEmbeddingService(EmbeddingProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        EmbeddingRequest request = new EmbeddingRequest(properties.getModel(), texts);
        EmbeddingResponse response = restClient.post()
                .uri("/embeddings")
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalStateException("Embedding 服务返回了空响应");
        }

        List<EmbeddingData> orderedData = response.data().stream()
                .sorted(Comparator.comparingInt(EmbeddingData::index))
                .toList();

        List<float[]> embeddings = orderedData.stream()
                .map(EmbeddingData::embedding)
                .toList();

        validateDimensions(embeddings);
        return embeddings;
    }

    @Override
    public float[] embed(String text) {
        List<float[]> embeddings = embed(List.of(text));
        return embeddings.isEmpty() ? new float[0] : embeddings.get(0);
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    private void validateDimensions(List<float[]> embeddings) {
        int expectedDimension = properties.getDimension();
        if (expectedDimension <= 0) {
            return;
        }

        for (float[] embedding : embeddings) {
            if (embedding.length != expectedDimension) {
                throw new IllegalStateException(
                        "Embedding 维度不一致，期望 " + expectedDimension + "，实际 " + embedding.length
                );
            }
        }
    }
}
