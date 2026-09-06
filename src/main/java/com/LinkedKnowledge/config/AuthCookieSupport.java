package com.LinkedKnowledge.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuthCookieSupport {
    private final AuthCookieProperties props;

    public void setToken(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(token, props.getMaxAgeSeconds()).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", 0).toString());
    }

    private ResponseCookie cookie(String value, int maxAgeSeconds) {
        return ResponseCookie.from(props.getName(), value == null ? "" : value)
                .httpOnly(props.isHttpOnly())
                .secure(props.isSecure())
                .path(props.getPath())
                .maxAge(Duration.ofSeconds(Math.max(0, maxAgeSeconds)))
                .sameSite(props.getSameSite())
                .build();
    }
}
