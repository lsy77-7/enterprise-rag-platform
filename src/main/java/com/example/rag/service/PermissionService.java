package com.example.rag.service;

import com.example.rag.common.exception.ResourceNotFoundException;
import com.example.rag.entity.KnowledgeBase;
import com.example.rag.entity.SysUser;
import com.example.rag.repository.KnowledgeBaseRepository;
import com.example.rag.repository.SysUserRepository;
import com.example.rag.repository.UserKnowledgeBaseRepository;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    private final SysUserRepository sysUserRepository;
    private final UserKnowledgeBaseRepository userKnowledgeBaseRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public PermissionService(
            SysUserRepository sysUserRepository,
            UserKnowledgeBaseRepository userKnowledgeBaseRepository,
            KnowledgeBaseRepository knowledgeBaseRepository
    ) {
        this.sysUserRepository = sysUserRepository;
        this.userKnowledgeBaseRepository = userKnowledgeBaseRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    public boolean canAccessKnowledgeBase(Long userId, Long knowledgeBaseId) {
        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + userId));
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + knowledgeBaseId));
        return userId.equals(knowledgeBase.getCreatedBy().getId())
                || userKnowledgeBaseRepository.existsByUserIdAndKnowledgeBaseId(userId, knowledgeBaseId);
    }

    public void assertCanAccessKnowledgeBase(Long userId, Long knowledgeBaseId) {
        if (!canAccessKnowledgeBase(userId, knowledgeBaseId)) {
            throw new ResourceNotFoundException("无权访问该知识库: " + knowledgeBaseId);
        }
    }

    public boolean canEditKnowledgeBase(Long userId, Long knowledgeBaseId) {
        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + userId));
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }

        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + knowledgeBaseId));
        if (userId.equals(knowledgeBase.getCreatedBy().getId())) {
            return true;
        }

        String permission =
                userKnowledgeBaseRepository.findPermissionByUserIdAndKnowledgeBaseId(userId, knowledgeBaseId);
        return "EDIT".equalsIgnoreCase(permission);
    }

    public void assertCanEditKnowledgeBase(Long userId, Long knowledgeBaseId) {
        if (!canEditKnowledgeBase(userId, knowledgeBaseId)) {
            throw new ResourceNotFoundException("无权编辑该知识库: " + knowledgeBaseId);
        }
    }
}
