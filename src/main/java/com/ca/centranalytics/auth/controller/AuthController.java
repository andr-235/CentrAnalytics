package com.ca.centranalytics.auth.controller;

import com.ca.centranalytics.auth.dto.AuthRequest;
import com.ca.centranalytics.auth.dto.AuthResponse;
import com.ca.centranalytics.auth.dto.RegisterRequest;
import com.ca.centranalytics.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }
}