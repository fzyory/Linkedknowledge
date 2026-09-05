package com.LinkedKnowledge.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Void>> handleRuntimeException(RuntimeException e) {
        String msg = e.getMessage();
        if (msg == null) msg = "服务器错误";
        if ("无权限".equals(msg)) {
            return ResponseEntity.status(403).body(Result.error(403, msg));
        }
        if ("节点不存在".equals(msg) || "建议不存在".equals(msg) || "用户不存在".equals(msg)) {
            return ResponseEntity.status(404).body(Result.error(404, msg));
        }
        if (msg.contains("用户名或密码") || msg.contains("未登录") || msg.contains("token")) {
            return ResponseEntity.status(401).body(Result.error(401, msg));
        }
        if (msg.contains("频繁") || msg.contains("过多")) {
            return ResponseEntity.status(429).body(Result.error(429, msg));
        }
        if (msg.contains("验证码") || msg.contains("已存在") || msg.contains("格式") || msg.contains("至少")) {
            return ResponseEntity.status(400).body(Result.error(400, msg));
        }
        return ResponseEntity.status(500).body(Result.error(500, msg));
    }
}
