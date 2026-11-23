// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.config.auth;

import com.converge.api.shared.infrastructure.config.idempotency.IdempotencyFilter;
import com.converge.api.shared.presentation.exception.ErrorCode;
import com.converge.api.shared.infrastructure.exception.ErrorResponse;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuração de segurança da API (Spring Security).
 *
 * Define quem pode acessar o quê: a API é stateless (sem sessão), a autenticação
 * é feita por token do Firebase (FirebaseTokenAuthenticationFilter) e, depois dela,
 * roda o filtro de idempotência (IdempotencyFilter). Só algumas rotas são públicas
 * (consulta de e-mail disponível, actuator, health, swagger etc.); todo o resto
 * exige usuário autenticado. Também configura o CORS e a resposta 401 padrão.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthorizationConfig {

    private final FirebaseTokenAuthenticationFilter firebaseTokenAuthenticationFilter;
    private final IdempotencyFilter idempotencyFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Monta a cadeia de filtros de segurança:
     * - desliga o CSRF (não há sessão nem cookie de login, o token vai no header);
     * - ativa o CORS com a configuração de corsConfigurationSource;
     * - define a política STATELESS: nada de sessão no servidor;
     * - define a resposta 401 em JSON (INVALID_CREDENTIALS, "Não autenticado ou
     *   token inválido.") no mesmo formato ApplicationResponse do resto da API;
     * - libera sem login o GET /api/v1/users/email-availability e as rotas
     *   técnicas (/actuator, /health, /auth, /public, swagger e api-docs);
     *   qualquer outra rota exige autenticação;
     * - registra o filtro do Firebase antes do filtro padrão de login e o filtro
     *   de idempotência logo depois dele.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            ApplicationResponse<Void> body = ApplicationResponse.failure(
                                    new ErrorResponse(ErrorCode.INVALID_CREDENTIALS, "Não autenticado ou token inválido.")
                            );
                            response.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/email-availability").permitAll()
                        .requestMatchers(
                                "/actuator/**",
                                "/health",
                                "/auth/**",
                                "/public/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(firebaseTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(idempotencyFilter, FirebaseTokenAuthenticationFilter.class)
                .build();
    }

    /**
     * Desabilita o registro automático do IdempotencyFilter como filtro comum do
     * servlet. Como ele é um bean, o Spring Boot o registraria sozinho, e ele
     * rodaria duas vezes: uma fora e outra dentro da cadeia do Spring Security.
     * Assim ele só roda onde foi encaixado (depois do filtro do Firebase).
     */
    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilterRegistration(IdempotencyFilter filter) {
        FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Define as regras de CORS para todas as rotas: aceita qualquer origem
     * (por padrão de origem), os métodos GET, POST, PUT, PATCH, DELETE e OPTIONS,
     * qualquer header, e permite credenciais.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
