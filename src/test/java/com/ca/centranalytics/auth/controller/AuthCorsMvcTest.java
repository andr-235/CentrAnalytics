package com.ca.centranalytics.auth.controller;

import com.ca.centranalytics.auth.security.CustomUserDetailsService;
import com.ca.centranalytics.auth.security.JwtAuthenticationFilter;
import com.ca.centranalytics.auth.security.SecurityConfig;
import com.ca.centranalytics.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthCorsMvcTest.TestBeans.class})
class AuthCorsMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authPreflightFromFrontendOriginAllowsCors() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        AuthService authService() {
            return new AuthService(null, null, null, null);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return new JwtAuthenticationFilter(null, null);
        }

        @Bean
        CustomUserDetailsService customUserDetailsService() {
            return new CustomUserDetailsService(null);
        }
    }
}
