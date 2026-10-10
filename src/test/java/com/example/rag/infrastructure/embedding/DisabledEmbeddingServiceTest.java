package com.example.rag.infrastructure.embedding;

import com.example.rag.common.exception.EmbeddingNotConfiguredException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DisabledEmbeddingServiceTest {

    private final DisabledEmbeddingService embeddingService = new DisabledEmbeddingService();

    @Test
    void shouldReportNotConfigured() {
        assertFalse(embeddingService.isConfigured());
    }

    @Test
    void shouldThrowClearExceptionWhenEmbeddingIsDisabled() {
        assertThrows(EmbeddingNotConfiguredException.class, () -> embeddingService.embed("hello"));
    }
}
