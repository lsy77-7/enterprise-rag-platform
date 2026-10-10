package com.example.rag.infrastructure.llm;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private boolean enabled;
    private String baseUrl;
    private String apiKey;
    private String model;

    public boolean isConfigured() {
        return enabled
                && StringUtils.hasText(baseUrl)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(model);
    }
}
