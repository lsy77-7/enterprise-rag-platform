package com.example.rag.infrastructure.parser;

import java.util.List;

public record ParsedDocument(List<ParsedPage> pages) {

    public ParsedDocument {
        pages = List.copyOf(pages);
    }

    public int pageCount() {
        return pages.size();
    }
}
