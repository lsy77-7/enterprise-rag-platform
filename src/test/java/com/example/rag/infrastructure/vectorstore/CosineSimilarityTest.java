package com.example.rag.infrastructure.vectorstore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CosineSimilarityTest {

    @Test
    void shouldReturnOneForIdenticalVectors() {
        double score = CosineSimilarity.calculate(new float[]{1, 2, 3}, new float[]{1, 2, 3});
        assertEquals(1.0, score, 0.000001);
    }

    @Test
    void shouldReturnZeroForOrthogonalVectors() {
        double score = CosineSimilarity.calculate(new float[]{1, 0}, new float[]{0, 1});
        assertEquals(0.0, score, 0.000001);
    }

    @Test
    void shouldRejectDifferentLengthVectors() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CosineSimilarity.calculate(new float[]{1, 0}, new float[]{1, 0, 1})
        );
    }

    @Test
    void shouldReturnZeroForZeroVector() {
        double score = CosineSimilarity.calculate(new float[]{0, 0}, new float[]{1, 1});
        assertEquals(0.0, score, 0.000001);
    }

    @Test
    void shouldRejectEmptyVector() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CosineSimilarity.calculate(new float[0], new float[0])
        );
    }
}
