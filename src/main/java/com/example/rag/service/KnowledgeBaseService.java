package com.example.rag.service;

import com.example.rag.common.exception.ResourceNotFoundException;
import com.example.rag.dto.request.BatchGrantRequest;
import com.example.rag.dto.request.BatchRevokeRequest;
import com.example.rag.dto.request.KnowledgeBaseCreateRequest;
import com.example.rag.dto.response.BatchGrantResult;
import com.example.rag.dto.response.BatchRevokeResult;
import com.example.rag.dto.response.KnowledgeBaseResponse;
import com.example.rag.dto.response.KnowledgeBaseGrantResponse;
import com.example.rag.entity.Document;
import com.example.rag.entity.KnowledgeBase;
import com.example.rag.entity.SysUser;
import com.example.rag.entity.UserKnowledgeBase;
import com.example.rag.entity.UserKnowledgeBaseId;
import com.example.rag.infrastructure.storage.FileStorageService;
import com.example.rag.infrastructure.vectorstore.VectorStoreService;
import com.example.rag.repository.DocumentChunkRepository;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.repository.KnowledgeBaseRepository;
import com.example.rag.repository.SysUserRepository;
import com.example.rag.repository.UserKnowledgeBaseRepository;
import com.example.rag.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DefaultUserService defaultUserService;
    private final VectorStoreService vectorStoreService;
    private final FileStorageService fileStorageService;
    private final SysUserRepository sysUserRepository;
    private final UserKnowledgeBaseRepository userKnowledgeBaseRepository;
    private final PermissionService permissionService;
    private final CurrentUserService currentUserService;
    private final VectorIndexingService vectorIndexingService;

    public KnowledgeBaseService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            DefaultUserService defaultUserService,
            VectorStoreService vectorStoreService,
            FileStorageService fileStorageService,
            SysUserRepository sysUserRepository,
            UserKnowledgeBaseRepository userKnowledgeBaseRepository,
            PermissionService permissionService,
            CurrentUserService currentUserService,
            VectorIndexingService vectorIndexingService
    ) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.defaultUserService = defaultUserService;
        this.vectorStoreService = vectorStoreService;
        this.fileStorageService = fileStorageService;
        this.sysUserRepository = sysUserRepository;
        this.userKnowledgeBaseRepository = userKnowledgeBaseRepository;
        this.permissionService = permissionService;
        this.currentUserService = currentUserService;
        this.vectorIndexingService = vectorIndexingService;
    }

    @Transactional
    public KnowledgeBaseResponse create(KnowledgeBaseCreateRequest request) {
        CurrentUser currentUser = currentUserService.requireUser();
        SysUser owner = sysUserRepository.getReferenceById(currentUser.id());

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setVisibility(resolveVisibility(currentUser));
        knowledgeBase.setCreatedBy(owner);

        return KnowledgeBaseResponse.from(knowledgeBaseRepository.save(knowledgeBase));
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBaseResponse> list() {
        CurrentUser currentUser = currentUserService.requireUser();

        List<KnowledgeBase> knowledgeBases;
        if ("ADMIN".equalsIgnoreCase(currentUser.role())) {
            knowledgeBases = knowledgeBaseRepository.findAll();
        } else {
            List<KnowledgeBase> owned =
                    knowledgeBaseRepository.findByCreatedBy_IdOrderByCreatedAtDesc(currentUser.id());
            List<Long> authorizedIds =
                    userKnowledgeBaseRepository.findKnowledgeBaseIdsByUserId(currentUser.id());
            List<KnowledgeBase> granted = authorizedIds.isEmpty()
                    ? List.of()
                    : knowledgeBaseRepository.findAllById(authorizedIds);

            knowledgeBases = new java.util.ArrayList<>();
            knowledgeBases.addAll(owned);
            knowledgeBases.addAll(granted);
            knowledgeBases = knowledgeBases.stream()
                    .distinct()
                    .sorted(java.util.Comparator.comparing(KnowledgeBase::getCreatedAt,
                            java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                    .toList();
        }

        return knowledgeBases.stream()
                .map(KnowledgeBaseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeBaseResponse getById(Long id) {
        permissionService.assertCanAccessKnowledgeBase(currentUserService.requireUserId(), id);
        KnowledgeBase knowledgeBase = getKnowledgeBase(id);
        return KnowledgeBaseResponse.from(knowledgeBase);
    }

    @Transactional
    public void delete(Long id) {
        CurrentUser currentUser = currentUserService.requireUser();
        KnowledgeBase knowledgeBase = getKnowledgeBase(id);
        if (!"ADMIN".equalsIgnoreCase(currentUser.role())
                && !currentUser.id().equals(knowledgeBase.getCreatedBy().getId())) {
            throw new ResourceNotFoundException("无权删除该知识库: " + id);
        }

        List<Document> documents =
                documentRepository.findByKnowledgeBase_IdOrderByCreatedAtDesc(id);

        for (Document document : documents) {
            documentChunkRepository.deleteByDocument_Id(document.getId());
            vectorStoreService.deleteByDocumentId(document.getId());
            fileStorageService.delete(document.getFilePath());
        }

        documentRepository.deleteAll(documents);
        knowledgeBaseRepository.delete(knowledgeBase);
        vectorStoreService.deleteByKnowledgeBaseId(id);
    }

    public void reindex(Long knowledgeBaseId) {
        permissionService.assertCanEditKnowledgeBase(
                currentUserService.requireUserId(),
                knowledgeBaseId
        );
        vectorIndexingService.reindexAsync(knowledgeBaseId);
    }

    public void grantAccess(Long knowledgeBaseId, Long userId, String permission) {
        UserKnowledgeBase grant = new UserKnowledgeBase(
                new UserKnowledgeBaseId(userId, knowledgeBaseId),
                normalizePermission(permission)
        );
        userKnowledgeBaseRepository.save(grant);
    }

    @Transactional
    public void revokeAccess(Long knowledgeBaseId, Long userId) {
        userKnowledgeBaseRepository.deleteByUserIdAndKnowledgeBaseId(userId, knowledgeBaseId);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBaseGrantResponse> listGrants(Long knowledgeBaseId) {
        getKnowledgeBase(knowledgeBaseId);

        List<UserKnowledgeBase> grants =
                userKnowledgeBaseRepository.findByKnowledgeBaseId(knowledgeBaseId);
        if (grants.isEmpty()) {
            return List.of();
        }

        Map<Long, String> permissionByUser = new LinkedHashMap<>();
        for (UserKnowledgeBase grant : grants) {
            permissionByUser.put(grant.getId().getUserId(), grant.getPermission());
        }

        return sysUserRepository.findAllById(permissionByUser.keySet()).stream()
                .map(user -> new KnowledgeBaseGrantResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        permissionByUser.get(user.getId())
                ))
                .sorted(Comparator.comparing(
                        KnowledgeBaseGrantResponse::getUsername,
                        Comparator.nullsLast(String::compareTo)
                ))
                .toList();
    }

    @Transactional
    public BatchGrantResult batchGrant(Long knowledgeBaseId, BatchGrantRequest request) {
        getKnowledgeBase(knowledgeBaseId);

        List<Long> distinctUserIds = distinctNonNull(request.getUserIds());
        if (distinctUserIds.isEmpty()) {
            return new BatchGrantResult(0, 0, List.of());
        }

        Set<Long> existingUserIds = new HashSet<>();
        for (SysUser user : sysUserRepository.findAllById(distinctUserIds)) {
            existingUserIds.add(user.getId());
        }

        List<Long> invalidUserIds = distinctUserIds.stream()
                .filter(id -> !existingUserIds.contains(id))
                .toList();

        String normalizedPermission = normalizePermission(request.getPermission());
        for (Long userId : existingUserIds) {
            userKnowledgeBaseRepository.save(new UserKnowledgeBase(
                    new UserKnowledgeBaseId(userId, knowledgeBaseId),
                    normalizedPermission
            ));
        }

        return new BatchGrantResult(
                existingUserIds.size(),
                invalidUserIds.size(),
                invalidUserIds
        );
    }

    @Transactional
    public BatchRevokeResult batchRevoke(Long knowledgeBaseId, BatchRevokeRequest request) {
        getKnowledgeBase(knowledgeBaseId);

        List<Long> distinctUserIds = distinctNonNull(request.getUserIds());
        int revokedCount = 0;
        for (Long userId : distinctUserIds) {
            revokedCount += userKnowledgeBaseRepository.deleteByUserIdAndKnowledgeBaseId(
                    userId,
                    knowledgeBaseId
            );
        }
        return new BatchRevokeResult(revokedCount);
    }

    private List<Long> distinctNonNull(List<Long> userIds) {
        if (userIds == null) {
            return List.of();
        }
        return userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String normalizePermission(String permission) {
        return "EDIT".equalsIgnoreCase(permission) ? "EDIT" : "READ";
    }

    private String resolveVisibility(CurrentUser currentUser) {
        return "ADMIN".equalsIgnoreCase(currentUser.role())
                ? KnowledgeBase.VISIBILITY_PUBLIC
                : KnowledgeBase.VISIBILITY_PRIVATE;
    }

    private KnowledgeBase getKnowledgeBase(Long id) {
        return knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + id));
    }
}
