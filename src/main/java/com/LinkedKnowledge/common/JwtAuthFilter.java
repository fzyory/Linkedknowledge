package com.LinkedKnowledge.common;
import java.io.IOException;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component                    // Spring 管理，自动实例化
@RequiredArgsConstructor      // 对 final 字段生成构造器（构造器注入）
public class JwtAuthFilter extends OncePerRequestFilter {

    // 注入你自己的 JWT 工具类（需要先写好 JwtUtil）
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String,String> redisTemplate;


    // === 核心方法：每个请求进来都会走这里 ===
    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,    // ① 进来的请求
            HttpServletResponse response,   // ② 返回给客户端的响应
            FilterChain         filterChain  // ③ 过滤器链——放行用的
    ) throws ServletException, IOException {

        // === 1. 公开接口直接放行（不查 token）===
        //    原因:浏览器会带上旧的/失效的 token cookie,如果不过滤掉,
        //    即使是 permitAll 的接口也会被拦截返回 401
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/login") ||
            path.startsWith("/api/auth/register") ||
            path.startsWith("/api/auth/sms")) {
            filterChain.doFilter(request, response);
            return;
        }

        // === 2. 从 Cookie 里取出 JWT ===
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

        // === 3. 没找到 token → 401(不放行) ===
        //     之前是"直接放行让 Controller 自己判断",但这导致:
        //     (a) 受保护接口没 token 时行为不一致
        //     (b) 注销 / 改密码后,前端跳登录页,用户再访问受保护接口 → 仍能"放行"
        //     现在统一:没 token = 未登录,直接 401
        if (token == null) {
            writeUnauthorized(response, "未登录或 token 无效");
            return;
        }

        // === 4. 验证 token ===
        //     jwtUtil.validateToken(token) 你要在 JwtUtil 里实现：
        //       ① 解析 token
        //       ② 验签名（Signature）
        //       ③ 检查是否过期（exp）
        // 4.1 查 redis 黑名单（在 validateToken 之前检查,被拉黑的 token 直接拒绝）
        if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
            writeUnauthorized(response, "token已失效,请重新登录");
            return;
        }
        // 4.2 验签 + 检查过期
        if (!jwtUtil.validateToken(token)) {
            writeUnauthorized(response, "token无效或已过期");
            return;
        }

        // === 5. token 有效 → 取出用户信息 ===
        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsernameFromToken(token);

        // === 6. 把用户信息存入 SecurityContext ===
        //     这样 Controller 里就能拿到"当前是谁在请求"
        //     SecurityContextHolder 是全局的，但底层用 ThreadLocal
        //     所以每个请求的 SecurityContext 是隔离的
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null ,null);
        authentication.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // === 7. 放行 ===
        filterChain.doFilter(request, response);
    }

    // 写 401 响应,格式跟 Result 一致,前端拦截器能正确解析
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> body = Result.error(401, message);
        response.getWriter().write(
            "{\"code\":" + body.getCode()
            + ",\"message\":\"" + body.getMessage()
            + "\",\"data\":null}"
        );
    }
}
