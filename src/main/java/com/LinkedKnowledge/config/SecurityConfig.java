package com.LinkedKnowledge.config;

import com.LinkedKnowledge.common.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration                     // 告诉 Spring 这是配置类
@EnableWebSecurity                 // 启用 Spring Security
@RequiredArgsConstructor
public class SecurityConfig {

    // 注入 JWT 过滤器，@RequiredArgsConstructor 会自动生成构造器
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    // === 2. 核心：定义安全规则链 ===
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // 2.1 关闭 CSRF（前后端分离的 API 不需要）
        http.csrf(csrf -> csrf.disable());

        // 2.2 设置 Session 策略为 STATELESS（无状态）
        //     用 JWT 就不需要服务器端的 Session 了
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 2.3 配置哪些接口需要认证、哪些公开
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/register")
                                .permitAll()                       // 登录注册 → 公开
                                .requestMatchers("/api/mindmap/**")
                                .permitAll()                       // 思维导图生成 → 公开（AI 接口）
                                .anyRequest()
                                .authenticated()                   // 其余 → 需要登录
        );

        // 2.4 把你的 JwtAuthFilter 插到 Spring Security 过滤器链里
        //     放在 UsernamePasswordAuthenticationFilter 之前
        http.addFilterBefore(jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class);

        // 2.5 构建返回
        return http.build();
    }
}
