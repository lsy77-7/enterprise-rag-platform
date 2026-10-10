package com.example.rag.service;

import com.example.rag.dto.request.LoginRequest;
import com.example.rag.entity.SysUser;
import com.example.rag.repository.SysUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void disabledUserCannotLogin() {
        SysUserRepository userRepository = mock(SysUserRepository.class);
        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        SysUser user = new SysUser();
        user.setUsername("disabled");
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setRole("USER");
        user.setStatus("DISABLED");

        when(userRepository.findByUsername("disabled")).thenReturn(Optional.of(user));

        AuthService authService = new AuthService(userRepository, passwordEncoder);
        LoginRequest request = new LoginRequest();
        request.setUsername("disabled");
        request.setPassword("123456");

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }
}
