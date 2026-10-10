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
public class UserPageResponse {

    private List<UserResponse> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
