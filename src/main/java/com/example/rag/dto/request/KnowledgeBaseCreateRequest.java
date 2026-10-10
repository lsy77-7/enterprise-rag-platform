package com.example.rag.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KnowledgeBaseCreateRequest {

    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 128, message = "知识库名称不能超过 128 个字符")
    private String name;

    @Size(max = 512, message = "知识库描述不能超过 512 个字符")
    private String description;
}
