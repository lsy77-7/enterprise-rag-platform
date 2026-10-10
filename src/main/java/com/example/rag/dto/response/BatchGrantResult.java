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
public class BatchGrantResult {

    private int grantedCount;
    private int invalidCount;
    private List<Long> invalidUserIds;
}
