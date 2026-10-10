package com.example.rag.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RetrievalSearchRequest {

    @NotNull(message = "knowledgeBaseId 不能为空")
    private Long knowledgeBaseId;

    @NotBlank(message = "query 不能为空")
    private String query;

    @Min(value = 1, message = "topK 不能小于 1")
    @Max(value = 20, message = "topK 不能大于 20")
    private Integer topK = 5;
}
