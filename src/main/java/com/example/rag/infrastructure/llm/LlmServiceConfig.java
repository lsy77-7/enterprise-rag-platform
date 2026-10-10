package com.example.rag.infrastructure.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmServiceConfig {

    @Bean
    @ConditionalOnProperty(name = "llm.enabled", havingValue = "true")
    public LlmService deepSeekLlmService(LlmProperties properties) {
        return new DeepSeekLlmService(properties);
    }

    @Bean
    @ConditionalOnMissingBean(LlmService.class)
    public LlmService disabledLlmService() {
        return new DisabledLlmService();
    }
}
