package com.example.rag.infrastructure.embedding;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {

    private boolean enabled;
    private String baseUrl;
    private String apiKey;
    private String model;
    private int dimension;
    private boolean autoIndex;

    public boolean isConfigured() {
        return enabled
                && StringUtils.hasText(baseUrl)
                && StringUtils.hasText(model);
    }
}
