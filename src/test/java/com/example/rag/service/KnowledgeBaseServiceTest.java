package com.example.rag.service;

import com.example.rag.entity.Document;
import com.example.rag.entity.KnowledgeBase;
import com.example.rag.infrastructure.storage.FileStorageService;
import com.example.rag.infrastructure.vectorstore.VectorStoreService;
import com.example.rag.repository.SysUserRepository;
import com.example.rag.repository.UserKnowledgeBaseRepository;
import com.example.rag.security.CurrentUser;
import com.example.rag.repository.DocumentChunkRepository;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.repository.KnowledgeBaseRepository;
import com.example.rag.dto.request.KnowledgeBaseCreateRequest;
import com.example.rag.dto.request.BatchGrantRequest;
import com.example.rag.dto.request.BatchRevokeRequest;
import com.example.rag.dto.response.BatchGrantResult;
import com.example.rag.dto.response.BatchRevokeResult;
import com.example.rag.entity.SysUser;
import com.example.rag.entity.UserKnowledgeBase;
import com.example.rag.dto.response.KnowledgeBaseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KnowledgeBaseServiceTest {

    private KnowledgeBaseRepository knowledgeBaseRepository;
    private DocumentRepository documentRepository;
    private DocumentChunkRepository documentChunkRepository;
    private VectorStoreService vectorStoreService;
    private FileStorageService fileStorageService;
    private PermissionService permissionService;
    private CurrentUserService currentUserService;
    private SysUserRepository sysUserRepository;
    private UserKnowledgeBaseRepository userKnowledgeBaseRepository;
    private KnowledgeBaseService service;

    @BeforeEach
    void setUp() {
        knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        documentRepository = mock(DocumentRepository.class);
        documentChunkRepository = mock(DocumentChunkRepository.class);
        vectorStoreService = mock(VectorStoreService.class);
        fileStorageService = mock(FileStorageService.class);
        permissionService = mock(PermissionService.class);
        currentUserService = mock(CurrentUserService.class);
        sysUserRepository = mock(SysUserRepository.class);
        userKnowledgeBaseRepository = mock(UserKnowledgeBaseRepository.class);
        when(currentUserService.requireUser()).thenReturn(
                new CurrentUser(1L, "admin", "管理员", "ADMIN", "ACTIVE")
        );
        when(currentUserService.requireUserId()).thenReturn(1L);

        service = new KnowledgeBaseService(
                knowledgeBaseRepository,
                documentRepository,
                documentChunkRepository,
                mock(DefaultUserService.class),
                vectorStoreService,
                fileStorageService,
                sysUserRepository,
                userKnowledgeBaseRepository,
                permissionService,
                currentUserService,
                mock(VectorIndexingService.class)
        );
    }

    @Test
    void shouldDeleteKnowledgeBaseAndCleanupAllRelatedData() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(1L);

        Document document = new Document();
        document.setId(10L);
        document.setFilePath("data/documents/10.pdf");

        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(knowledgeBase));
        when(documentRepository.findByKnowledgeBase_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(document));

        service.delete(1L);

        verify(documentChunkRepository).deleteByDocument_Id(10L);
        verify(vectorStoreService).deleteByDocumentId(10L);
        verify(fileStorageService).delete("data/documents/10.pdf");
        verify(documentRepository).deleteAll(any());
        verify(knowledgeBaseRepository).delete(knowledgeBase);
        verify(vectorStoreService).deleteByKnowledgeBaseId(1L);
    }

    @Test
    void adminCanCreatePublicKnowledgeBase() {
        SysUser admin = new SysUser();
        admin.setId(1L);
        admin.setRole("ADMIN");
        when(sysUserRepository.getReferenceById(1L)).thenReturn(admin);
        when(knowledgeBaseRepository.save(any(KnowledgeBase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeBaseCreateRequest request = new KnowledgeBaseCreateRequest();
        request.setName("公开知识库");

        KnowledgeBaseResponse response = service.create(request);

        assertEquals(KnowledgeBase.VISIBILITY_PUBLIC, response.getVisibility());
    }

    @Test
    void normalUserCreatesPrivateKnowledgeBase() {
        CurrentUserService userCurrentUserService = mock(CurrentUserService.class);
        when(userCurrentUserService.requireUser()).thenReturn(
                new CurrentUser(2L, "alice", "爱丽丝", "USER", "ACTIVE")
        );
        when(userCurrentUserService.requireUserId()).thenReturn(2L);

        SysUserRepository userRepo = mock(SysUserRepository.class);
        SysUser user = new SysUser();
        user.setId(2L);
        user.setRole("USER");
        when(userRepo.getReferenceById(2L)).thenReturn(user);

        KnowledgeBaseRepository kbRepo = mock(KnowledgeBaseRepository.class);
        when(kbRepo.save(any(KnowledgeBase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeBaseService userService = new KnowledgeBaseService(
                kbRepo,
                mock(DocumentRepository.class),
                mock(DocumentChunkRepository.class),
                mock(DefaultUserService.class),
                mock(VectorStoreService.class),
                mock(FileStorageService.class),
                userRepo,
                mock(UserKnowledgeBaseRepository.class),
                permissionService,
                userCurrentUserService,
                mock(VectorIndexingService.class)
        );

        KnowledgeBaseCreateRequest request = new KnowledgeBaseCreateRequest();
        request.setName("我的私有知识库");

        KnowledgeBaseResponse response = userService.create(request);

        assertEquals(KnowledgeBase.VISIBILITY_PRIVATE, response.getVisibility());
    }

    @Test
    void shouldBatchGrantExistingUsersAndReportInvalid() {
        when(knowledgeBaseRepository.findById(1L))
                .thenReturn(Optional.of(new KnowledgeBase()));

        SysUser alice = new SysUser();
        alice.setId(2L);
        alice.setUsername("alice");
        SysUser bob = new SysUser();
        bob.setId(3L);
        bob.setUsername("bob");
        when(sysUserRepository.findAllById(List.of(2L, 3L, 999L)))
                .thenReturn(List.of(alice, bob));

        BatchGrantRequest request = new BatchGrantRequest();
        request.setUserIds(List.of(2L, 3L, 999L));
        request.setPermission("READ");

        BatchGrantResult result = service.batchGrant(1L, request);

        assertEquals(2, result.getGrantedCount());
        assertEquals(1, result.getInvalidCount());
        assertEquals(List.of(999L), result.getInvalidUserIds());

        ArgumentCaptor<UserKnowledgeBase> captor =
                ArgumentCaptor.forClass(UserKnowledgeBase.class);
        verify(userKnowledgeBaseRepository, times(2)).save(captor.capture());
        List<UserKnowledgeBase> saved = captor.getAllValues();
        assertEquals(List.of(2L, 3L), saved.stream()
                .map(item -> item.getId().getUserId())
                .sorted()
                .toList());
        assertEquals(List.of("READ", "READ"), saved.stream()
                .map(UserKnowledgeBase::getPermission)
                .sorted()
                .toList());
    }

    @Test
    void shouldBatchRevokeUsers() {
        when(knowledgeBaseRepository.findById(1L))
                .thenReturn(Optional.of(new KnowledgeBase()));
        when(userKnowledgeBaseRepository.deleteByUserIdAndKnowledgeBaseId(2L, 1L)).thenReturn(1);
        when(userKnowledgeBaseRepository.deleteByUserIdAndKnowledgeBaseId(3L, 1L)).thenReturn(1);

        BatchRevokeRequest request = new BatchRevokeRequest();
        request.setUserIds(List.of(2L, 3L));

        BatchRevokeResult result = service.batchRevoke(1L, request);

        assertEquals(2, result.getRevokedCount());
    }
}
