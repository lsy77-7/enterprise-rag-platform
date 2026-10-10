package com.example.rag.service;

import com.example.rag.entity.SysUser;
import com.example.rag.repository.SysUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultUserService {

    private static final String DEFAULT_USERNAME = "admin";

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultUserService(SysUserRepository sysUserRepository, PasswordEncoder passwordEncoder) {
        this.sysUserRepository = sysUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SysUser getOrCreateDefaultUser() {
        return sysUserRepository.findByUsername(DEFAULT_USERNAME)
                .orElseGet(this::createDefaultUser);
    }

    private SysUser createDefaultUser() {
        SysUser user = new SysUser();
        user.setUsername(DEFAULT_USERNAME);
        user.setPasswordHash(passwordEncoder.encode("admin123"));
        user.setDisplayName("系统管理员");
        user.setRole("ADMIN");
        user.setStatus("ACTIVE");
        return sysUserRepository.save(user);
    }
}
