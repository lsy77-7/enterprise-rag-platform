package com.example.rag.infrastructure.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EmbeddingDtosTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapOpenAiCompatibleEmbeddingResponse() throws Exception {
        String json = """
                {
                  "object": "list",
                  "data": [
                    {
                      "object": "embedding",
                      "index": 0,
                      "embedding": [0.1, 0.2, 0.3]
                    }
                  ],
                  "model": "BAAI/bge-m3",
                  "usage": {
                    "prompt_tokens": 3,
                    "total_tokens": 3
                  }
                }
                """;

        EmbeddingResponse response = objectMapper.readValue(json, EmbeddingResponse.class);

        assertNotNull(response);
        assertEquals(1, response.data().size());
        assertEquals(0, response.data().get(0).index());
        assertEquals(3, response.data().get(0).embedding().length);
        assertEquals("BAAI/bge-m3", response.model());
        assertEquals(3, response.usage().totalTokens());
    }
}
