package com.example.rag.infrastructure.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StoredFile {

    private final String originalFileName;
    private final String storedFileName;
    private final String filePath;
    private final String contentType;
    private final long size;
}
