package com.example.rag.repository;

import com.example.rag.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocument_IdOrderByChunkIndexAsc(Long documentId);

    void deleteByDocument_Id(Long documentId);

    @Query("""
            select c
            from DocumentChunk c
            join c.document d
            where d.knowledgeBase.id = :knowledgeBaseId
            order by d.id asc, c.chunkIndex asc
            """)
    List<DocumentChunk> findByKnowledgeBaseIdOrderByDocumentIdAndChunkIndex(
            @Param("knowledgeBaseId") Long knowledgeBaseId
    );
}
