package com.ca.centranalytics.auth.controller;

import com.ca.centranalytics.auth.dto.AuthRequest;
import com.ca.centranalytics.auth.dto.AuthResponse;
import com.ca.centranalytics.auth.dto.RegisterRequest;
import com.ca.centranalytics.auth.service.AuthService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @RateLimiter(name = "registerEndpoint")
    public AuthResponse register(@RequestBody @Valid RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @RateLimiter(name = "authEndpoint")
    public AuthResponse login(@RequestBody @Valid AuthRequest request) {
        return authService.login(request);
    }
}