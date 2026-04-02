package com.ca.centranalytics.auth.security;

import com.ca.centranalytics.auth.config.JwtProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("c3VwZXJfc2VjcmV0X2tleV9mb3Jfand0X3Rva2VuX3NpZ25pbmdfYW5kX3ZhbGlkYXRpb24=");
        properties.setExpiration(86400000L);
        jwtService = new JwtService(properties);
    }

    @Test
    void testGenerateToken_Success() {
        UserDetails userDetails = User.builder()
                .username("testuser")
                .password("password123")
                .roles("USER")
                .build();

        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testExtractUsername_Success() {
        UserDetails userDetails = User.builder()
                .username("testuser")
                .password("password123")
                .roles("USER")
                .build();

        String token = jwtService.generateToken(userDetails);
        String username = jwtService.extractUsername(token);

        assertEquals("testuser", username);
    }

    @Test
    void testIsTokenValid_Success() {
        UserDetails userDetails = User.builder()
                .username("testuser")
                .password("password123")
                .roles("USER")
                .build();

        String token = jwtService.generateToken(userDetails);

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void testIsTokenValid_InvalidUser() {
        UserDetails userDetails = User.builder()
                .username("testuser")
                .password("password123")
                .roles("USER")
                .build();

        UserDetails otherUser = User.builder()
                .username("otheruser")
                .password("password123")
                .roles("USER")
                .build();

        String token = jwtService.generateToken(userDetails);

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void testExtractUsername_InvalidToken() {
        String invalidToken = "invalid.token.here";

        assertThrows(JwtException.class, () -> {
            jwtService.extractUsername(invalidToken);
        });
    }

    @Test
    void testExtractUsername_ExpiredToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("c3VwZXJfc2VjcmV0X2tleV9mb3Jfand0X3Rva2VuX3NpZ25pbmdfYW5kX3ZhbGlkYXRpb24=");
        properties.setExpiration(-1L);
        JwtService expiredJwtService = new JwtService(properties);

        UserDetails userDetails = User.builder()
                .username("expired-user")
                .password("password123")
                .roles("USER")
                .build();

        String token = expiredJwtService.generateToken(userDetails);

        assertThrows(ExpiredJwtException.class, () -> expiredJwtService.extractUsername(token));
    }
}
