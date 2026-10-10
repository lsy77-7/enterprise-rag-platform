package com.example.rag.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalSearchResponse {

    private Long chunkId;
    private Long documentId;
    private Long knowledgeBaseId;
    private Integer chunkIndex;
    private Integer pageNumber;
    private Double score;
    private String content;
}
