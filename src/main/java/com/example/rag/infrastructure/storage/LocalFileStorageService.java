package com.example.rag.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.storage.documents-dir:data/documents}")
    private String documentsDir;

    @Override
    public StoredFile store(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "document" : file.getOriginalFilename()
        );
        String extension = StringUtils.getFilenameExtension(originalFileName);
        if (extension == null || extension.isBlank()) {
            extension = "pdf";
        }

        String storedFileName = UUID.randomUUID() + "." + extension.toLowerCase();

        try {
            Path root = Path.of(documentsDir).toAbsolutePath().normalize();
            Files.createDirectories(root);
            Path target = root.resolve(storedFileName).normalize();

            if (!target.startsWith(root)) {
                throw new IllegalStateException("非法文件存储路径");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return new StoredFile(
                    originalFileName,
                    storedFileName,
                    target.toString(),
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException ex) {
            throw new IllegalStateException("文件保存失败", ex);
        }
    }

    @Override
    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException ignored) {
            // 删除失败不覆盖原始业务异常，保持文件存储操作尽力而为。
        }
    }
}
