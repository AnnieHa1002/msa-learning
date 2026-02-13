package com.sparta.msa_learning.auth.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class AuthConfig {

    // SecurityFilterChain 빈을 정의합니다. 이 메서드는 Spring Security의 보안 필터 체인을 구성합니다.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 보호를 비활성화합니다. CSRF 보호는 주로 브라우저 클라이언트를 대상으로 하는 공격을 방지하기 위해 사용됩니다.
                .csrf(AbstractHttpConfigurer::disable)
                // 요청에 대한 접근 권한을 설정합니다.
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                // "/api/auth/**" 경로에 대한 모든 요청을 허용합니다.
                                .requestMatchers("/api/auth/**").permitAll()
                                // 그 외의 모든 요청에 대해서는 인증을 요구합니다.
                                .anyRequest().authenticated()
                )

                // 세션 관리 정책을 정의합니다. 여기서는 세션을 사용하지 않도록 STATELESS로 설정합니다.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        // 설정된 보안 필터 체인을 반환합니다.
        return http.build();
    }
}