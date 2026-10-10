package com.example.rag.service;

import com.example.rag.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public CurrentUser requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser user)) {
            throw new IllegalStateException("用户未登录");
        }
        return user;
    }

    public Long requireUserId() {
        return requireUser().id();
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(requireUser().role());
    }
}
