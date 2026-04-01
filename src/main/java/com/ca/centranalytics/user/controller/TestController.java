package com.ca.centranalytics.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public String test() {
        return "JWT работает";
    }

    @GetMapping("/")
    public String root() {
        return "Root endpoint - требуется аутентификация";
    }
}