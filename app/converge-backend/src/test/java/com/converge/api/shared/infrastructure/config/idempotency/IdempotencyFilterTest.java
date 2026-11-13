// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.config.idempotency;

import com.converge.api.shared.domain.enums.IdempotencyStatus;
import com.converge.api.shared.domain.model.IdempotencyKey;
import com.converge.api.shared.domain.repository.IdempotencyKeyRepository;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    @Mock
    private IdempotencyKeyRepository repository;

    @Mock
    private FilterChain filterChain;

    private IdempotencyFilter filter;

    @BeforeEach
    void setUp() {
        filter = new IdempotencyFilter(repository);
        AuthenticatedUser user = new AuthenticatedUser("user-1", "user@puc.br");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve armazenar resposta e fingerprint no primeiro POST com Idempotency-Key e retornar resposta em cache no replay")
    void shouldStoreAndReplayCompletedResponse() throws ServletException, IOException {
        AtomicReference<IdempotencyKey> savedEntry = new AtomicReference<>();

        MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/v1/rides/ride-1/passengers/p1/accept");
        request1.addHeader("Idempotency-Key", "uuid-123");
        request1.setQueryString("notify=true");
        request1.setContent("{\"note\":\"ok\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response1 = new MockHttpServletResponse();

        when(repository.save(any(IdempotencyKey.class))).thenAnswer(inv -> {
            IdempotencyKey arg = inv.getArgument(0);
            savedEntry.set(arg);
            return arg;
        });
        doAnswer(inv -> {
            HttpServletRequest req = (HttpServletRequest) inv.getArgument(0);
            assertThat(req.getInputStream().readAllBytes()).isEqualTo("{\"note\":\"ok\"}".getBytes(StandardCharsets.UTF_8));
            HttpServletResponse wrapper = (HttpServletResponse) inv.getArgument(1);
            wrapper.setStatus(200);
            wrapper.setContentType("application/json");
            wrapper.getWriter().write("{\"success\":true,\"data\":{\"id\":\"ride-1\"}}");
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request1, response1, filterChain);

        assertThat(response1.getStatus()).isEqualTo(200);
        assertThat(response1.getContentAsString()).isEqualTo("{\"success\":true,\"data\":{\"id\":\"ride-1\"}}");
        assertThat(savedEntry.get().getRequestFingerprint()).isNotBlank();
        verify(repository, times(2)).save(any(IdempotencyKey.class));
        verify(filterChain, times(1)).doFilter(any(), any());

        // Replay com a mesma chave, query e corpo
        when(repository.save(any(IdempotencyKey.class))).thenThrow(new DuplicateKeyException("duplicate key"));
        when(repository.findByKey("uuid-123")).thenReturn(Optional.of(savedEntry.get()));

        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/v1/rides/ride-1/passengers/p1/accept");
        request2.addHeader("Idempotency-Key", "uuid-123");
        request2.setQueryString("notify=true");
        request2.setContent("{\"note\":\"ok\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        filter.doFilter(request2, response2, filterChain);

        assertThat(response2.getStatus()).isEqualTo(200);
        assertThat(response2.getContentAsString()).isEqualTo("{\"success\":true,\"data\":{\"id\":\"ride-1\"}}");
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando Idempotency-Key é reutilizada com corpo ou query diferente")
    void shouldReturnBadRequestWhenFingerprintDiffers() throws ServletException, IOException {
        AtomicReference<IdempotencyKey> savedEntry = new AtomicReference<>();
        when(repository.save(any(IdempotencyKey.class))).thenAnswer(inv -> {
            IdempotencyKey arg = inv.getArgument(0);
            savedEntry.set(arg);
            return arg;
        });

        MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/v1/rides/request");
        request1.addHeader("Idempotency-Key", "uuid-fp");
        request1.setContent("{\"driverRouteId\":\"r1\"}".getBytes(StandardCharsets.UTF_8));
        filter.doFilter(request1, new MockHttpServletResponse(), filterChain);

        when(repository.save(any(IdempotencyKey.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(repository.findByKey("uuid-fp")).thenReturn(Optional.of(savedEntry.get()));

        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/v1/rides/request");
        request2.addHeader("Idempotency-Key", "uuid-fp");
        request2.setContent("{\"driverRouteId\":\"r2\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        filter.doFilter(request2, response2, filterChain);

        assertThat(response2.getStatus()).isEqualTo(400);
        assertThat(response2.getContentAsString()).contains("BAD_REQUEST");
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Deve preservar reserva como UNKNOWN_OUTCOME se falhar ao salvar resposta concluída após mutação")
    void shouldPreserveUnknownOutcomeWhenSavingCompletedFails() throws ServletException, IOException {
        AtomicReference<IdempotencyKey> savedEntry = new AtomicReference<>();
        when(repository.save(any(IdempotencyKey.class)))
                .thenAnswer(inv -> {
                    IdempotencyKey arg = inv.getArgument(0);
                    savedEntry.set(arg);
                    return arg;
                })
                .thenThrow(new RuntimeException("Mongo timeout on save"))
                .thenAnswer(inv -> {
                    IdempotencyKey arg = inv.getArgument(0);
                    savedEntry.set(arg);
                    return arg;
                });

        doAnswer(inv -> {
            HttpServletResponse wrapper = (HttpServletResponse) inv.getArgument(1);
            wrapper.setStatus(200);
            wrapper.getWriter().write("{\"success\":true}");
            return null;
        }).when(filterChain).doFilter(any(), any());

        MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/v1/rides/ride-1/cancel");
        request1.addHeader("Idempotency-Key", "uuid-unk");
        MockHttpServletResponse response1 = new MockHttpServletResponse();

        filter.doFilter(request1, response1, filterChain);

        assertThat(response1.getStatus()).isEqualTo(200);
        assertThat(savedEntry.get().getStatus()).isEqualTo(IdempotencyStatus.UNKNOWN_OUTCOME);
        verify(repository, never()).deleteByKey("uuid-unk");

        // Retry com a mesma chave não deve reexecutar mutação
        when(repository.save(any(IdempotencyKey.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(repository.findByKey("uuid-unk")).thenReturn(Optional.of(savedEntry.get()));

        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/v1/rides/ride-1/cancel");
        request2.addHeader("Idempotency-Key", "uuid-unk");
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        filter.doFilter(request2, response2, filterChain);

        assertThat(response2.getStatus()).isEqualTo(409);
        assertThat(response2.getContentAsString()).contains("RIDE_CONFLICT");
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando requisição com mesma Idempotency-Key ainda está IN_PROGRESS")
    void shouldReturnConflictWhenInProgress() throws ServletException, IOException {
        AtomicReference<IdempotencyKey> savedEntry = new AtomicReference<>();
        when(repository.save(any(IdempotencyKey.class))).thenAnswer(inv -> {
            IdempotencyKey arg = inv.getArgument(0);
            savedEntry.set(arg);
            return arg;
        });

        // Simula primeira requisição registrando IN_PROGRESS
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/rides/request");
        req.addHeader("Idempotency-Key", "uuid-456");
        req.setContent("{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        filter.doFilter(req, new MockHttpServletResponse(), filterChain);

        savedEntry.set(IdempotencyKey.inProgress(
                "uuid-456", "user-1", "POST", "/api/v1/rides/request", savedEntry.get().getRequestFingerprint()));

        when(repository.save(any(IdempotencyKey.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(repository.findByKey("uuid-456")).thenReturn(Optional.of(savedEntry.get()));

        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/v1/rides/request");
        request2.addHeader("Idempotency-Key", "uuid-456");
        request2.setContent("{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        filter.doFilter(request2, response2, filterChain);

        assertThat(response2.getStatus()).isEqualTo(409);
        assertThat(response2.getContentAsString()).contains("RIDE_CONFLICT");
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Deve remover chave de idempotência se ocorrer erro 500 para permitir retry")
    void shouldDeleteKeyOnServerError() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/rides/ride-1/cancel");
        request.addHeader("Idempotency-Key", "uuid-500");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(repository.save(any(IdempotencyKey.class))).thenAnswer(inv -> inv.getArgument(0));
        doAnswer(inv -> {
            HttpServletResponse wrapper = (HttpServletResponse) inv.getArgument(1);
            wrapper.setStatus(500);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        verify(repository).deleteByKey("uuid-500");
    }

    @Test
    @DisplayName("Deve retornar 413 quando o corpo da requisição exceder o limite configurado")
    void shouldReturn413WhenBodyExceedsMaxBodyBytes() throws ServletException, IOException {
        IdempotencyFilter smallLimitFilter = new IdempotencyFilter(repository, 16);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/rides/request");
        request.addHeader("Idempotency-Key", "uuid-too-large");
        request.setContent("0123456789abcdef_overflow".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        smallLimitFilter.doFilter(request, response, filterChain);

        assertEquals(413, response.getStatus());
        verify(repository, never()).save(any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Não deve filtrar GET nem endpoints fora de /api/v1/rides")
    void shouldSkipGetAndNonRidePaths() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/v1/rides"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/api/v1/routes"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/api/v1/rides/request"))).isFalse();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("PATCH", "/api/v1/rides/1/cancel"))).isFalse();
    }
}
