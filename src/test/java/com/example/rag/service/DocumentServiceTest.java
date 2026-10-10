package com.example.rag.service;

import com.example.rag.common.exception.ResourceNotFoundException;
import com.example.rag.entity.Document;
import com.example.rag.entity.KnowledgeBase;
import com.example.rag.infrastructure.parser.PdfDocumentParser;
import com.example.rag.infrastructure.splitter.SimpleTextSplitter;
import com.example.rag.infrastructure.storage.FileStorageService;
import com.example.rag.infrastructure.vectorstore.VectorStoreService;
import com.example.rag.repository.DocumentChunkRepository;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceTest {

    private DocumentRepository documentRepository;
    private KnowledgeBaseRepository knowledgeBaseRepository;
    private DocumentChunkRepository documentChunkRepository;
    private PermissionService permissionService;
    private CurrentUserService currentUserService;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        documentChunkRepository = mock(DocumentChunkRepository.class);
        permissionService = mock(PermissionService.class);
        currentUserService = mock(CurrentUserService.class);

        service = new DocumentService(
                documentRepository,
                knowledgeBaseRepository,
                mock(FileStorageService.class),
                mock(DefaultUserService.class),
                mock(PdfDocumentParser.class),
                mock(SimpleTextSplitter.class),
                documentChunkRepository,
                mock(VectorStoreService.class),
                mock(VectorIndexingService.class),
                mock(AfterCommitExecutor.class),
                permissionService,
                currentUserService
        );
    }

    @Test
    void shouldListDocumentsWithReadPermissionScopedToKnowledgeBase() {
        when(currentUserService.requireUserId()).thenReturn(1L);
        when(knowledgeBaseRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.findByKnowledgeBase_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        service.list(1L);

        verify(permissionService).assertCanAccessKnowledgeBase(1L, 1L);
        verify(documentRepository).findByKnowledgeBase_IdOrderByCreatedAtDesc(1L);
        verify(permissionService, never()).assertCanEditKnowledgeBase(anyLong(), anyLong());
    }

    @Test
    void shouldRejectDeletingDocumentFromAnotherKnowledgeBase() {
        KnowledgeBase otherKnowledgeBase = new KnowledgeBase();
        otherKnowledgeBase.setId(2L);

        Document document = new Document();
        document.setId(10L);
        document.setKnowledgeBase(otherKnowledgeBase);

        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));

        assertThrows(ResourceNotFoundException.class, () -> service.delete(1L, 10L));

        verify(documentRepository, never()).delete(any(Document.class));
        verify(documentChunkRepository, never()).deleteByDocument_Id(anyLong());
    }

    @Test
    void shouldRejectReindexingDocumentFromAnotherKnowledgeBase() {
        KnowledgeBase otherKnowledgeBase = new KnowledgeBase();
        otherKnowledgeBase.setId(2L);

        Document document = new Document();
        document.setId(10L);
        document.setKnowledgeBase(otherKnowledgeBase);

        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));

        assertThrows(ResourceNotFoundException.class, () -> service.reindexDocument(1L, 10L));
    }
}
