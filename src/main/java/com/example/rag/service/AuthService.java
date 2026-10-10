package com.example.rag.service;

import com.example.rag.dto.request.LoginRequest;
import com.example.rag.dto.response.CurrentUserResponse;
import com.example.rag.entity.SysUser;
import com.example.rag.repository.SysUserRepository;
import com.example.rag.security.CurrentUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(SysUserRepository sysUserRepository, PasswordEncoder passwordEncoder) {
        this.sysUserRepository = sysUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CurrentUserResponse login(LoginRequest request) {
        SysUser user = sysUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("用户已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        CurrentUser currentUser = toCurrentUser(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        currentUser,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return CurrentUserResponse.from(currentUser);
    }

    public CurrentUserResponse me(CurrentUser currentUser) {
        return CurrentUserResponse.from(currentUser);
    }

    private CurrentUser toCurrentUser(SysUser user) {
        return new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus()
        );
    }
}
