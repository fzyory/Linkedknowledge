package com.LinkedKnowledge.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class HelloController {
    @GetMapping("/hello")
    public String sayHello() {
        // 3. 返回给用户的内容
        return "Hello, Linked Knowledge!";
    }
}


