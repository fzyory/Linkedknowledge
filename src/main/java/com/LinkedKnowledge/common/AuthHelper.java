package com.LinkedKnowledge.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 取当前登录用户 id。
 * JwtAuthFilter 已把 userId 放进 authentication.getDetails()。
 */
public final class AuthHelper {

    private AuthHelper() {}

    /**
     * @return 当前用户 id；未登录或 details 不是 Long 时返回 null
     */
    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null) {
            return null;
        }
        Object details = auth.getDetails();
        if (details instanceof Long userId) {
            return userId;
        }
        return null;
    }
}
