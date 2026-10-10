package com.example.rag.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFile store(MultipartFile file);

    void delete(String filePath);
}
