package com.LinkedKnowledge.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class HelloController {

    @GetMapping("/hello")
    public String sayHello() {
        // 测试学习记录系统 - 这是一个测试注释
        return "Hello, Linked Knowledge! 学习记录系统测试中...";
    }
}


