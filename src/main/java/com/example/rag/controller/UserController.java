package com.example.rag.controller;

import com.example.rag.common.ApiResponse;
import com.example.rag.dto.request.BatchUserCreateRequest;
import com.example.rag.dto.request.UserCreateRequest;
import com.example.rag.dto.request.UserUpdateRequest;
import com.example.rag.dto.response.BatchUserCreateResult;
import com.example.rag.dto.response.UserPageResponse;
import com.example.rag.dto.response.UserResponse;
import com.example.rag.security.CurrentUser;
import com.example.rag.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<UserPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(userService.list(page, size, username, role, status));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    @PostMapping("/batch")
    public ApiResponse<BatchUserCreateResult> batchCreate(
            @RequestBody BatchUserCreateRequest request
    ) {
        return ApiResponse.success(userService.batchCreate(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request
    ) {
        return ApiResponse.success(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        userService.delete(id, currentUser.id());
        return ApiResponse.success(null);
    }
}
