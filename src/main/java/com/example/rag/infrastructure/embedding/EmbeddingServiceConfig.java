package com.example.rag.infrastructure.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingServiceConfig {

    @Bean
    @ConditionalOnProperty(name = "embedding.enabled", havingValue = "true")
    public EmbeddingService httpEmbeddingService(EmbeddingProperties properties) {
        return new HttpEmbeddingService(properties);
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingService.class)
    public EmbeddingService disabledEmbeddingService() {
        return new DisabledEmbeddingService();
    }
}
