package com.example.rag.repository;

import com.example.rag.entity.UserKnowledgeBase;
import com.example.rag.entity.UserKnowledgeBaseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserKnowledgeBaseRepository extends JpaRepository<UserKnowledgeBase, UserKnowledgeBaseId> {

    @Query("select g.id.knowledgeBaseId from UserKnowledgeBase g where g.id.userId = :userId")
    List<Long> findKnowledgeBaseIdsByUserId(@Param("userId") Long userId);

    @Query("select g.id.userId from UserKnowledgeBase g where g.id.knowledgeBaseId = :knowledgeBaseId")
    List<Long> findUserIdsByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);

    @Query("""
            select count(g) > 0
            from UserKnowledgeBase g
            where g.id.userId = :userId and g.id.knowledgeBaseId = :knowledgeBaseId
            """)
    boolean existsByUserIdAndKnowledgeBaseId(
            @Param("userId") Long userId,
            @Param("knowledgeBaseId") Long knowledgeBaseId
    );

    @Query("""
            select g.permission
            from UserKnowledgeBase g
            where g.id.userId = :userId and g.id.knowledgeBaseId = :knowledgeBaseId
            """)
    String findPermissionByUserIdAndKnowledgeBaseId(
            @Param("userId") Long userId,
            @Param("knowledgeBaseId") Long knowledgeBaseId
    );

    @Query("""
            select g
            from UserKnowledgeBase g
            where g.id.knowledgeBaseId = :knowledgeBaseId
            """)
    List<UserKnowledgeBase> findByKnowledgeBaseId(
            @Param("knowledgeBaseId") Long knowledgeBaseId
    );

    @Modifying
    @Query("delete from UserKnowledgeBase g where g.id.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("""
            delete from UserKnowledgeBase g
            where g.id.userId = :userId and g.id.knowledgeBaseId = :knowledgeBaseId
            """)
    int deleteByUserIdAndKnowledgeBaseId(
            @Param("userId") Long userId,
            @Param("knowledgeBaseId") Long knowledgeBaseId
    );
}
