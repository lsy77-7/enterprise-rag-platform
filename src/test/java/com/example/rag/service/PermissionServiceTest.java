package com.example.rag.service;

import com.example.rag.common.exception.ResourceNotFoundException;
import com.example.rag.entity.KnowledgeBase;
import com.example.rag.entity.SysUser;
import com.example.rag.repository.SysUserRepository;
import com.example.rag.repository.UserKnowledgeBaseRepository;
import com.example.rag.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionServiceTest {

    private SysUserRepository sysUserRepository;
    private UserKnowledgeBaseRepository userKnowledgeBaseRepository;
    private KnowledgeBaseRepository knowledgeBaseRepository;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        sysUserRepository = mock(SysUserRepository.class);
        userKnowledgeBaseRepository = mock(UserKnowledgeBaseRepository.class);
        knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        permissionService = new PermissionService(
                sysUserRepository,
                userKnowledgeBaseRepository,
                knowledgeBaseRepository
        );
    }

    @Test
    void adminCanAccessAllKnowledgeBases() {
        SysUser admin = user(1L, "ADMIN");
        when(sysUserRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertTrue(permissionService.canAccessKnowledgeBase(1L, 999L));
    }

    @Test
    void userCanOnlyAccessGrantedKnowledgeBase() {
        SysUser user = user(2L, "USER");
        when(sysUserRepository.findById(2L)).thenReturn(Optional.of(user));
        when(knowledgeBaseRepository.findById(3L)).thenReturn(Optional.of(knowledgeBase(3L)));
        when(knowledgeBaseRepository.findById(4L)).thenReturn(Optional.of(knowledgeBase(4L)));
        when(userKnowledgeBaseRepository.existsByUserIdAndKnowledgeBaseId(2L, 3L))
                .thenReturn(true);
        when(userKnowledgeBaseRepository.existsByUserIdAndKnowledgeBaseId(2L, 4L))
                .thenReturn(false);

        assertTrue(permissionService.canAccessKnowledgeBase(2L, 3L));
        assertFalse(permissionService.canAccessKnowledgeBase(2L, 4L));
    }

    @Test
    void userCannotAccessUnauthorizedKnowledgeBase() {
        SysUser user = user(2L, "USER");
        when(sysUserRepository.findById(2L)).thenReturn(Optional.of(user));
        when(knowledgeBaseRepository.findById(4L)).thenReturn(Optional.of(knowledgeBase(4L)));
        when(userKnowledgeBaseRepository.existsByUserIdAndKnowledgeBaseId(2L, 4L))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> permissionService.assertCanAccessKnowledgeBase(2L, 4L)
        );
    }

    private SysUser user(Long id, String role) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setRole(role);
        user.setStatus("ACTIVE");
        return user;
    }

    private KnowledgeBase knowledgeBase(Long id) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setCreatedBy(user(99L, "USER"));
        return knowledgeBase;
    }
}
