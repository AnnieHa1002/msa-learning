package com.sparta.msa_learning.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class AuthService {

    @Value("${spring.application.name}")
    private String issuer;

    @Value("${service.jwt.access-expiration}")
    private Long accessExpiration;

    private final SecretKey secretKey;


    public AuthService(@Value("${service.jwt.secret-key}") String secretKey) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
    }


    public String createAccessToken(String userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(issuer)
                .claim("user_id", userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessExpiration))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String createAuthToken(String username) {
        return createAccessToken(username);
    }
}
