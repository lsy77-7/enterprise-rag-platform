package com.example.rag.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchUserCreateResult {

    private int total;
    private int successCount;
    private int failureCount;
    private List<String> failures;
}
