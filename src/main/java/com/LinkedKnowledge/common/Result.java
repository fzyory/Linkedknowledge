package com.LinkedKnowledge.common;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    // 显式传类型，避免 T 推断失败
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static Result<Void> success() {
        return new Result<>(200, "操作成功", null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
