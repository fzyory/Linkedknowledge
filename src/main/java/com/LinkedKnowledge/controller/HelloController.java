package com.LinkedKnowledge.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class HelloController {

    @GetMapping("/hello")
    public String sayHello() {
        // 第二次测试 - 验证hooks自动触发
        return "Hello, Linked Knowledge! Hooks测试第2轮";
    }
}


