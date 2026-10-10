package com.example.rag.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConversationRenameRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 50, message = "标题不能超过 50 个字符")
    private String title;
}
