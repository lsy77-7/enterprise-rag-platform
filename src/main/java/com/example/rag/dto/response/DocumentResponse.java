package com.example.rag.dto.response;

import com.example.rag.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private Long id;
    private String fileName;
    private String status;
    private Integer pageCount;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFileName(),
                document.getStatus(),
                document.getPageCount(),
                document.getErrorMessage(),
                document.getCreatedAt()
        );
    }
}
