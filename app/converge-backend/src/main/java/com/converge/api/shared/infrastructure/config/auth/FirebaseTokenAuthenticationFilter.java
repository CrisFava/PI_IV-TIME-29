package com.converge.api.shared.infrastructure.config.auth;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
                AuthenticatedUser principal = parseToken(token);
                if (principal != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    Collections.emptyList()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private AuthenticatedUser parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
                String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
                JsonNode payload = objectMapper.readTree(payloadJson);

                if (payload.hasNonNull("exp")) {
                    long exp = payload.get("exp").asLong();
                    if (Instant.now().getEpochSecond() >= exp) {
                        log.debug("Firebase ID token expired at {}", exp);
                        return null;
                    }
                }

                String uid = payload.hasNonNull("user_id")
                        ? payload.get("user_id").asString()
                        : (payload.hasNonNull("sub") ? payload.get("sub").asString() : null);
                String email = payload.hasNonNull("email") ? payload.get("email").asString() : null;

                if (uid != null && !uid.isBlank()) {
                    return new AuthenticatedUser(uid, email);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse Bearer token: {}", e.getMessage());
        }
        return null;
    }
}
