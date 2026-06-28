package com.LinkedKnowledge.common;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component                    // Spring 管理，自动实例化
@RequiredArgsConstructor      // 对 final 字段生成构造器（构造器注入）
public class JwtAuthFilter extends OncePerRequestFilter {

    // 注入你自己的 JWT 工具类（需要先写好 JwtUtil）
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String,String> redisTemplate;


    // === 2. 核心方法：每个请求进来都会走这里 ===
    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,    // ① 进来的请求
            HttpServletResponse response,   // ② 返回给客户端的响应
            FilterChain         filterChain  // ③ 过滤器链——放行用的
    ) throws ServletException, IOException {

        // === 2.1 从 Cookie 里取出 JWT ===
        String token = null;
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {   // Cookie 名叫 "token"
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // === 2.2 没找到 token → 直接放行 ===
        //     （后续 Controller 自己判断是访客还是登录用户）
        if (token == null) {
            filterChain.doFilter(request, response);  // 交给下一个过滤器
            return;
        }

        // === 2.3 验证 token ===
        //     jwtUtil.validateToken(token) 你要在 JwtUtil 里实现：
        //       ① 解析 token
        //       ② 验签名（Signature）
        //       ③ 检查是否过期（exp）
        //2.3.1查redis黑名单
        if(redisTemplate.hasKey("blacklist:" + token)){
            response.setStatus(401);
            response.getWriter().write("token has been revoked");
            return;
        }
        if (!jwtUtil.validateToken(token)) {
            // 签名无效或过期 → 返回 401
            response.setStatus(401);
            response.getWriter().write("token invalid or expired");
            return;  // 不放行
        }

        // === 2.4 token 有效 → 取出用户信息 ===
        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsernameFromToken(token);

        // === 2.5 把用户信息存入 SecurityContext ===
        //     这样 Controller 里就能拿到"当前是谁在请求"
        //     SecurityContextHolder 是全局的，但底层用 ThreadLocal
        //     所以每个请求的 SecurityContext 是隔离的
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, null);
        authentication.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // === 2.6 放行 ===
        filterChain.doFilter(request, response);
    }
}
