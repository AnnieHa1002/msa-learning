package com.sparta.msa_learning.auth.controller;

import com.sparta.msa_learning.auth.service.AuthService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor

    static class AuthResponse {
        private String accessToken;

    }

    @GetMapping("")
    public String authenticate() {
        return "auth service is running";
    }


    @GetMapping("/sign-in")
    public ResponseEntity<?> createAuthToken(@RequestParam String username) {

        return ResponseEntity.ok(new AuthResponse(authService.createAuthToken(username)));
    }
}
