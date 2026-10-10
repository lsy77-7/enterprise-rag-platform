package com.example.rag.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConversationCreateRequest {

    @NotNull(message = "knowledgeBaseId 不能为空")
    private Long knowledgeBaseId;

    @Size(max = 255, message = "会话标题不能超过 255 个字符")
    private String title;
}
