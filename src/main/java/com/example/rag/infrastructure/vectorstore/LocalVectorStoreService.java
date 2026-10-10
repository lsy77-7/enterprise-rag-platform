package com.example.rag.infrastructure.vectorstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class LocalVectorStoreService implements VectorStoreService {

    private final Path vectorStoreRoot;
    private final ObjectMapper objectMapper;
    private final Object lock = new Object();

    public LocalVectorStoreService(
            @Value("${app.vector-store.dir:data/vector-store}") String vectorStoreDir,
            ObjectMapper objectMapper
    ) {
        this.vectorStoreRoot = Path.of(vectorStoreDir).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(VectorRecord record) {
        saveAll(List.of(record));
    }

    @Override
    public void saveAll(List<VectorRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        synchronized (lock) {
            Map<Long, List<VectorRecord>> recordsByKnowledgeBase = new HashMap<>();
            for (VectorRecord record : records) {
                recordsByKnowledgeBase
                        .computeIfAbsent(record.knowledgeBaseId(), key -> new ArrayList<>())
                        .add(record);
            }

            for (Map.Entry<Long, List<VectorRecord>> entry : recordsByKnowledgeBase.entrySet()) {
                mergeAndWrite(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public List<VectorSearchResult> search(float[] queryVector, Long knowledgeBaseId, int topK) {
        if (knowledgeBaseId == null) {
            return search(queryVector, topK);
        }

        synchronized (lock) {
            Path file = fileForKnowledgeBase(knowledgeBaseId);
            if (!Files.exists(file)) {
                return List.of();
            }

            VectorIndexFile indexFile = readIndexFile(file);
            List<VectorRecord> scopedRecords = indexFile.records().stream()
                    .filter(record -> record.knowledgeBaseId() == knowledgeBaseId)
                    .toList();
            return rank(queryVector, scopedRecords, topK);
        }
    }

    @Override
    public List<VectorSearchResult> search(float[] queryVector, int topK) {
        synchronized (lock) {
            List<VectorRecord> allRecords = new ArrayList<>();
            try {
                if (!Files.exists(vectorStoreRoot)) {
                    return List.of();
                }

                try (Stream<Path> stream = Files.list(vectorStoreRoot)) {
                    List<Path> files = stream
                            .filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().endsWith(".json"))
                            .toList();

                    for (Path file : files) {
                        allRecords.addAll(readIndexFile(file).records());
                    }
                }
            } catch (IOException ex) {
                throw new IllegalStateException("读取向量索引失败", ex);
            }

            return rank(queryVector, allRecords, topK);
        }
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (documentId == null) {
            return;
        }

        synchronized (lock) {
            try {
                if (!Files.exists(vectorStoreRoot)) {
                    return;
                }

                try (Stream<Path> stream = Files.list(vectorStoreRoot)) {
                    List<Path> files = stream
                            .filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().endsWith(".json"))
                            .toList();

                    for (Path file : files) {
                        VectorIndexFile indexFile = readIndexFile(file);
                        List<VectorRecord> filtered = indexFile.records().stream()
                                .filter(record -> record.documentId() != documentId)
                                .toList();
                        writeIndexFile(file, new VectorIndexFile(indexFile.knowledgeBaseId(), filtered));
                    }
                }
            } catch (IOException ex) {
                throw new IllegalStateException("删除向量索引失败", ex);
            }
        }
    }

    @Override
    public void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return;
        }

        synchronized (lock) {
            try {
                Files.deleteIfExists(fileForKnowledgeBase(knowledgeBaseId));
            } catch (IOException ex) {
                throw new IllegalStateException("删除知识库向量索引失败", ex);
            }
        }
    }

    private void mergeAndWrite(Long knowledgeBaseId, List<VectorRecord> newRecords) {
        List<VectorRecord> validRecords = newRecords.stream()
                .filter(record -> record.knowledgeBaseId() == knowledgeBaseId)
                .toList();

        if (validRecords.size() != newRecords.size()) {
            throw new IllegalArgumentException("VectorRecord 中存在不属于该知识库的数据");
        }

        Path file = fileForKnowledgeBase(knowledgeBaseId);
        VectorIndexFile existing = Files.exists(file)
                ? readIndexFile(file)
                : new VectorIndexFile(knowledgeBaseId, List.of());

        Map<Long, VectorRecord> recordsByChunkId = new HashMap<>();
        for (VectorRecord record : existing.records()) {
            recordsByChunkId.put(record.chunkId(), record);
        }
        for (VectorRecord record : validRecords) {
            recordsByChunkId.put(record.chunkId(), record);
        }

        List<VectorRecord> merged = recordsByChunkId.values().stream()
                .sorted(Comparator
                        .comparingLong(VectorRecord::documentId)
                        .thenComparingInt(VectorRecord::chunkIndex))
                .toList();

        writeIndexFile(file, new VectorIndexFile(knowledgeBaseId, merged));
    }

    private List<VectorSearchResult> rank(float[] queryVector, List<VectorRecord> records, int topK) {
        if (topK <= 0) {
            return List.of();
        }

        return records.stream()
                .map(record -> new VectorSearchResult(
                        record.chunkId(),
                        record.documentId(),
                        record.knowledgeBaseId(),
                        record.chunkIndex(),
                        record.pageNumber(),
                        CosineSimilarity.calculate(queryVector, record.embedding()),
                        record.content()
                ))
                .sorted(Comparator.comparingDouble(VectorSearchResult::score).reversed())
                .limit(topK)
                .toList();
    }

    private VectorIndexFile readIndexFile(Path file) {
        try {
            return objectMapper.readValue(file.toFile(), VectorIndexFile.class);
        } catch (IOException ex) {
            throw new IllegalStateException("读取向量索引失败: " + file, ex);
        }
    }

    private void writeIndexFile(Path file, VectorIndexFile indexFile) {
        try {
            Files.createDirectories(file.getParent());
            Path tempFile = file.resolveSibling(file.getFileName().toString() + ".tmp");
            objectMapper.writeValue(tempFile.toFile(), indexFile);

            try {
                Files.move(
                        tempFile,
                        file,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("写入向量索引失败: " + file, ex);
        }
    }

    private Path fileForKnowledgeBase(Long knowledgeBaseId) {
        return vectorStoreRoot.resolve(knowledgeBaseId + ".json");
    }

    private record VectorIndexFile(long knowledgeBaseId, List<VectorRecord> records) {
    }
}
