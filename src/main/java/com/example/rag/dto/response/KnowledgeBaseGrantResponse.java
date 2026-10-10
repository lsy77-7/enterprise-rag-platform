package com.example.rag.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseGrantResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String permission;
}
