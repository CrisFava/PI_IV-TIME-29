package com.converge.api.shared.config.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthorizationConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http){
        return http
                .authorizeHttpRequests(auth -> auth
                                .requestMatchers(
                                        "/actuator/**",
                                        "/health",
                                        "/auth/**",
                                        "/public/**"
                                ).permitAll()
                        .anyRequest().authenticated()
                )
                .build();

    }
}
