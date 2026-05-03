package com.LinkedKnowledge.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class HelloController {

    @GetMapping("/hello")
    public String sayHello() {
        // 第三次测试 - 验证Git原生hooks
        return "Hello, Linked Knowledge! Git hooks测试";
    }
}


