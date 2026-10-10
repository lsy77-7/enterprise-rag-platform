package com.example.rag.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConversationChatRequest {

    @NotBlank(message = "question 不能为空")
    private String question;

    @Min(value = 1, message = "topK 不能小于 1")
    @Max(value = 20, message = "topK 不能大于 20")
    private Integer topK = 3;
}
