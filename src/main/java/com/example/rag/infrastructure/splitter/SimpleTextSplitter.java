package com.example.rag.infrastructure.splitter;

import com.example.rag.infrastructure.parser.ParsedPage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SimpleTextSplitter {

    private final int chunkSize;
    private final int overlap;

    public SimpleTextSplitter(
            @Value("${app.splitter.chunk-size:1000}") int chunkSize,
            @Value("${app.splitter.overlap:200}") int overlap
    ) {
        validate(chunkSize, overlap);
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public List<TextChunk> split(String text) {
        return split(List.of(new ParsedPage(1, text == null ? "" : text)));
    }

    public List<TextChunk> split(List<ParsedPage> pages) {
        List<TextChunk> chunks = new ArrayList<>();

        if (pages == null) {
            return chunks;
        }

        for (ParsedPage page : pages) {
            String pageText = page.text();
            if (pageText == null || pageText.isBlank()) {
                continue;
            }
            chunks.addAll(splitPageText(page.pageNumber(), pageText));
        }

        return chunks;
    }

    private List<TextChunk> splitPageText(int pageNumber, String text) {
        List<TextChunk> chunks = new ArrayList<>();
        String trimmed = text.strip();

        if (trimmed.isEmpty()) {
            return chunks;
        }

        if (trimmed.length() <= chunkSize) {
            chunks.add(new TextChunk(pageNumber, trimmed));
            return chunks;
        }

        int start = 0;
        while (start < trimmed.length()) {
            int preferredEnd = Math.min(start + chunkSize, trimmed.length());
            int end = preferredEnd == trimmed.length()
                    ? preferredEnd
                    : findBoundary(trimmed, start, preferredEnd);

            String content = trimmed.substring(start, end).strip();
            if (!content.isEmpty()) {
                chunks.add(new TextChunk(pageNumber, content));
            }

            if (end >= trimmed.length()) {
                break;
            }

            int nextStart = end - overlap;
            if (nextStart <= start) {
                nextStart = start + 1;
            }
            start = nextStart;
        }

        return chunks;
    }

    private int findBoundary(String text, int start, int preferredEnd) {
        int minimumBoundary = Math.max(start + 1, preferredEnd - Math.max(50, overlap));
        String[] boundaries = {"\n\n", "\n", "。", "！", "？"};

        for (String boundary : boundaries) {
            int index = text.lastIndexOf(boundary, preferredEnd);
            if (index >= minimumBoundary) {
                return Math.min(index + boundary.length(), preferredEnd);
            }
        }

        return preferredEnd;
    }

    private void validate(int chunkSize, int overlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize 必须大于 0");
        }
        if (overlap < 0) {
            throw new IllegalArgumentException("overlap 不能小于 0");
        }
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException("overlap 必须小于 chunkSize");
        }
    }
}
