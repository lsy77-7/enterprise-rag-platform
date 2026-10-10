package com.example.rag.service;

import com.example.rag.common.exception.ResourceNotFoundException;
import com.example.rag.dto.response.DocumentResponse;
import com.example.rag.dto.response.DocumentUploadResponse;
import com.example.rag.entity.Document;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.entity.KnowledgeBase;
import com.example.rag.entity.SysUser;
import com.example.rag.infrastructure.parser.ParsedDocument;
import com.example.rag.infrastructure.parser.PdfDocumentParser;
import com.example.rag.infrastructure.splitter.SimpleTextSplitter;
import com.example.rag.infrastructure.splitter.TextChunk;
import com.example.rag.infrastructure.storage.FileStorageService;
import com.example.rag.infrastructure.storage.StoredFile;
import com.example.rag.infrastructure.vectorstore.VectorStoreService;
import com.example.rag.repository.DocumentChunkRepository;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final FileStorageService fileStorageService;
    private final DefaultUserService defaultUserService;
    private final PdfDocumentParser pdfDocumentParser;
    private final SimpleTextSplitter simpleTextSplitter;
    private final DocumentChunkRepository documentChunkRepository;
    private final VectorStoreService vectorStoreService;
    private final VectorIndexingService vectorIndexingService;
    private final AfterCommitExecutor afterCommitExecutor;
    private final PermissionService permissionService;
    private final CurrentUserService currentUserService;

    public DocumentService(
            DocumentRepository documentRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            FileStorageService fileStorageService,
            DefaultUserService defaultUserService,
            PdfDocumentParser pdfDocumentParser,
            SimpleTextSplitter simpleTextSplitter,
            DocumentChunkRepository documentChunkRepository,
            VectorStoreService vectorStoreService,
            VectorIndexingService vectorIndexingService,
            AfterCommitExecutor afterCommitExecutor,
            PermissionService permissionService,
            CurrentUserService currentUserService
    ) {
        this.documentRepository = documentRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.fileStorageService = fileStorageService;
        this.defaultUserService = defaultUserService;
        this.pdfDocumentParser = pdfDocumentParser;
        this.simpleTextSplitter = simpleTextSplitter;
        this.documentChunkRepository = documentChunkRepository;
        this.vectorStoreService = vectorStoreService;
        this.vectorIndexingService = vectorIndexingService;
        this.afterCommitExecutor = afterCommitExecutor;
        this.permissionService = permissionService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public DocumentUploadResponse upload(Long knowledgeBaseId, MultipartFile file) {
        permissionService.assertCanEditKnowledgeBase(
                currentUserService.requireUserId(),
                knowledgeBaseId
        );
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + knowledgeBaseId));

        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "document" : file.getOriginalFilename()
        );
        String extension = StringUtils.getFilenameExtension(originalFileName);
        if (extension == null || !"pdf".equalsIgnoreCase(extension)) {
            throw new IllegalArgumentException("当前阶段仅支持 PDF 文件");
        }

        StoredFile storedFile = fileStorageService.store(file);

        try {
            SysUser owner = defaultUserService.getOrCreateDefaultUser();

            Document document = new Document();
            document.setKnowledgeBase(knowledgeBase);
            document.setName(storedFile.getOriginalFileName());
            document.setOriginalFileName(storedFile.getOriginalFileName());
            document.setFilePath(storedFile.getFilePath());
            document.setFileType(storedFile.getContentType());
            document.setFileSize(storedFile.getSize());
            document.setStatus("UPLOADED");
            document.setCreatedBy(owner);

            Document savedDocument = documentRepository.save(document);
            documentChunkRepository.deleteByDocument_Id(savedDocument.getId());

            try {
                savedDocument.setStatus("PARSING");
                documentRepository.save(savedDocument);

                ParsedDocument parsedDocument = pdfDocumentParser.parse(Path.of(storedFile.getFilePath()));
                savedDocument.setPageCount(parsedDocument.pageCount());
                savedDocument.setStatus("INDEXING");
                documentRepository.save(savedDocument);

                List<DocumentChunk> chunks = new ArrayList<>();
                List<TextChunk> textChunks = simpleTextSplitter.split(parsedDocument.pages());
                for (int index = 0; index < textChunks.size(); index++) {
                    TextChunk textChunk = textChunks.get(index);

                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setDocument(savedDocument);
                    chunk.setChunkIndex(index);
                    chunk.setContent(textChunk.content());
                    chunk.setCharacterCount(textChunk.content().length());
                    chunk.setPageNumber(textChunk.pageNumber());
                    chunks.add(chunk);
                }

                documentChunkRepository.saveAll(chunks);
                Long documentId = savedDocument.getId();
                afterCommitExecutor.executeAfterCommit(
                        () -> vectorIndexingService.indexDocumentAsync(documentId)
                );
                return DocumentUploadResponse.from(savedDocument);
            } catch (RuntimeException ex) {
                savedDocument.setStatus("FAILED");
                savedDocument.setErrorMessage(ex.getMessage());
                documentRepository.save(savedDocument);
                fileStorageService.delete(storedFile.getFilePath());
                return DocumentUploadResponse.from(savedDocument);
            }
        } catch (RuntimeException ex) {
            fileStorageService.delete(storedFile.getFilePath());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(Long knowledgeBaseId) {
        permissionService.assertCanAccessKnowledgeBase(
                currentUserService.requireUserId(),
                knowledgeBaseId
        );
        if (!knowledgeBaseRepository.existsById(knowledgeBaseId)) {
            throw new ResourceNotFoundException("知识库不存在: " + knowledgeBaseId);
        }

        return documentRepository.findByKnowledgeBase_IdOrderByCreatedAtDesc(knowledgeBaseId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long knowledgeBaseId, Long documentId) {
        permissionService.assertCanEditKnowledgeBase(
                currentUserService.requireUserId(),
                knowledgeBaseId
        );
        Document document = documentRepository.findById(documentId)
                .filter(item -> item.getKnowledgeBase().getId().equals(knowledgeBaseId))
                .orElseThrow(() -> new ResourceNotFoundException("文档不存在: " + documentId));

        documentChunkRepository.deleteByDocument_Id(documentId);
        vectorStoreService.deleteByDocumentId(documentId);
        documentRepository.delete(document);
        fileStorageService.delete(document.getFilePath());
    }

    @Transactional
    public void reindexDocument(Long knowledgeBaseId, Long documentId) {
        permissionService.assertCanAccessKnowledgeBase(
                currentUserService.requireUserId(),
                knowledgeBaseId
        );
        Document document = documentRepository.findById(documentId)
                .filter(item -> item.getKnowledgeBase().getId().equals(knowledgeBaseId))
                .orElseThrow(() -> new ResourceNotFoundException("文档不存在: " + documentId));

        afterCommitExecutor.executeAfterCommit(
                () -> vectorIndexingService.indexDocumentAsync(documentId)
        );
    }
}
