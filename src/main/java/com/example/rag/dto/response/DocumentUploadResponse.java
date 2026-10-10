package com.example.rag.dto.response;

import com.example.rag.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {

    private Long id;
    private String fileName;
    private String status;
    private String errorMessage;

    public static DocumentUploadResponse from(Document document) {
        return new DocumentUploadResponse(
                document.getId(),
                document.getOriginalFileName(),
                document.getStatus(),
                document.getErrorMessage()
        );
    }
}
