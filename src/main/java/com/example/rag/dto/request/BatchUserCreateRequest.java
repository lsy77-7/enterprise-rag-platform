package com.example.rag.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BatchUserCreateRequest {

    private List<UserCreateRequest> users;
}
