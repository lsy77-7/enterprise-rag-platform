package com.example.rag.controller;

import com.example.rag.common.ApiResponse;
import com.example.rag.dto.request.LoginRequest;
import com.example.rag.dto.response.CurrentUserResponse;
import com.example.rag.security.CurrentUser;
import com.example.rag.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<CurrentUserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session
    ) {
        CurrentUserResponse response = authService.login(request);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );
        return ApiResponse.success(response);
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.success(authService.me(currentUser));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.success(null);
    }
}
