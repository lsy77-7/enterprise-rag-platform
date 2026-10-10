package com.example.rag.infrastructure.splitter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleTextSplitterTest {

    @Test
    void shouldReturnEmptyListForEmptyText() {
        SimpleTextSplitter splitter = new SimpleTextSplitter(1000, 200);

        assertTrue(splitter.split("").isEmpty());
        assertTrue(splitter.split((String) null).isEmpty());
    }

    @Test
    void shouldKeepTextShorterThanChunkSizeAsOneChunk() {
        SimpleTextSplitter splitter = new SimpleTextSplitter(100, 20);

        List<TextChunk> chunks = splitter.split("短文本");

        assertEquals(1, chunks.size());
        assertEquals(1, chunks.get(0).pageNumber());
        assertEquals("短文本", chunks.get(0).content());
    }

    @Test
    void shouldSplitLongTextIntoMultipleChunks() {
        SimpleTextSplitter splitter = new SimpleTextSplitter(5, 0);

        List<TextChunk> chunks = splitter.split("abcdefghij");

        assertEquals(2, chunks.size());
        assertEquals("abcde", chunks.get(0).content());
        assertEquals("fghij", chunks.get(1).content());
    }

    @Test
    void shouldApplyOverlapBetweenChunks() {
        SimpleTextSplitter splitter = new SimpleTextSplitter(5, 2);

        List<TextChunk> chunks = splitter.split("abcdefghij");

        assertEquals("abcde", chunks.get(0).content());
        assertEquals("defgh", chunks.get(1).content());
        assertEquals("ghij", chunks.get(2).content());
    }

    @Test
    void shouldKeepLastPartialChunk() {
        SimpleTextSplitter splitter = new SimpleTextSplitter(4, 1);

        List<TextChunk> chunks = splitter.split("abcdefg");

        assertEquals(2, chunks.size());
        assertEquals("abcd", chunks.get(0).content());
        assertTrue(chunks.get(1).content().endsWith("g"));
    }

    @Test
    void shouldRejectInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> new SimpleTextSplitter(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new SimpleTextSplitter(100, -1));
        assertThrows(IllegalArgumentException.class, () -> new SimpleTextSplitter(100, 100));
    }
}
