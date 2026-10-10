package com.example.rag.repository;

import com.example.rag.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    List<KnowledgeBase> findByCreatedBy_IdOrderByCreatedAtDesc(Long createdById);
}
