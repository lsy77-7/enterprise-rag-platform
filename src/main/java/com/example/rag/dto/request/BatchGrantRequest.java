package com.example.rag.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BatchGrantRequest {

    @NotEmpty(message = "用户 ID 列表不能为空")
    private List<Long> userIds;

    private String permission = "READ";
}
