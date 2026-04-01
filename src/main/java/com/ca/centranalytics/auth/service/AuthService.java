package com.ca.centranalytics.auth.service;

import com.ca.centranalytics.auth.dto.AuthRequest;
import com.ca.centranalytics.auth.dto.AuthResponse;
import com.ca.centranalytics.auth.dto.RegisterRequest;
import com.ca.centranalytics.auth.security.JwtService;
import com.ca.centranalytics.user.entity.Role;
import com.ca.centranalytics.user.entity.User;
import com.ca.centranalytics.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username уже занят");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();

        String jwt = jwtService.generateToken(userDetails);
        return new AuthResponse(jwt);
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();

        String jwt = jwtService.generateToken(userDetails);
        return new AuthResponse(jwt);
    }
}