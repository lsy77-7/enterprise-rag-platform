package com.example.rag.service;

import com.example.rag.common.exception.ResourceNotFoundException;
import com.example.rag.dto.request.BatchUserCreateRequest;
import com.example.rag.dto.request.UserCreateRequest;
import com.example.rag.dto.request.UserUpdateRequest;
import com.example.rag.dto.response.BatchUserCreateResult;
import com.example.rag.dto.response.UserPageResponse;
import com.example.rag.dto.response.UserResponse;
import com.example.rag.entity.SysUser;
import com.example.rag.repository.ConversationRepository;
import com.example.rag.repository.SysUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;

@Service
public class UserService {

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConversationRepository conversationRepository;

    public UserService(
            SysUserRepository sysUserRepository,
            PasswordEncoder passwordEncoder,
            ConversationRepository conversationRepository
    ) {
        this.sysUserRepository = sysUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.conversationRepository = conversationRepository;
    }

    @Transactional(readOnly = true)
    public UserPageResponse list(
            int page,
            int size,
            String username,
            String role,
            String status
    ) {
        Specification<SysUser> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(username)) {
                predicates.add(builder.like(
                        builder.lower(root.get("username")),
                        "%" + username.toLowerCase() + "%"
                ));
            }
            if (StringUtils.hasText(role)) {
                predicates.add(builder.equal(root.get("role"), role));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<SysUser> result = sysUserRepository.findAll(specification, pageable);

        return new UserPageResponse(
                result.getContent().stream().map(UserResponse::from).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        sysUserRepository.findByUsername(request.getUsername()).ifPresent(user -> {
            throw new IllegalArgumentException("用户名已存在");
        });

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getNickname());
        user.setRole(normalizeRole(request.getRole()));
        user.setStatus(normalizeStatus(request.getStatus()));
        return UserResponse.from(sysUserRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        SysUser user = getUser(id);

        if (StringUtils.hasText(request.getNickname())) {
            user.setDisplayName(request.getNickname());
        }
        if (StringUtils.hasText(request.getRole())) {
            user.setRole(normalizeRole(request.getRole()));
        }
        if (StringUtils.hasText(request.getStatus())) {
            user.setStatus(normalizeStatus(request.getStatus()));
        }

        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Long id, Long currentUserId) {
        SysUser user = getUser(id);
        if (id.equals(currentUserId)) {
            throw new IllegalArgumentException("不能删除当前登录管理员");
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())
                && "ACTIVE".equalsIgnoreCase(user.getStatus())
                && sysUserRepository.countByRoleAndStatus("ADMIN", "ACTIVE") <= 1) {
            throw new IllegalArgumentException("不能删除系统最后一个有效管理员");
        }
        conversationRepository.deleteByUserId(id);
        sysUserRepository.delete(user);
    }

    public BatchUserCreateResult batchCreate(BatchUserCreateRequest request) {
        List<UserCreateRequest> users = request.getUsers() == null
                ? List.of()
                : request.getUsers();
        List<String> failures = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < users.size(); i++) {
            UserCreateRequest item = users.get(i);
            try {
                create(item);
                successCount++;
            } catch (Exception ex) {
                failures.add("第 " + (i + 1) + " 条失败：" + ex.getMessage());
            }
        }

        return new BatchUserCreateResult(
                users.size(),
                successCount,
                failures.size(),
                failures
        );
    }

    private SysUser getUser(Long id) {
        return sysUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + id));
    }

    private String normalizeRole(String role) {
        return "ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "USER";
    }

    private String normalizeStatus(String status) {
        return "DISABLED".equalsIgnoreCase(status) ? "DISABLED" : "ACTIVE";
    }
}
