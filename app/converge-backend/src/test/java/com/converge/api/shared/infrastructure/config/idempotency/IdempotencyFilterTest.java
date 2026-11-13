// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.config.idempotency;

import com.converge.api.shared.domain.model.IdempotencyKey;
import com.converge.api.shared.domain.repository.IdempotencyKeyRepository;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("Deve armazenar resposta no primeiro POST com Idempotency-Key e retornar resposta em cache no replay")
    void shouldStoreAndReplayCompletedResponse() throws ServletException, IOException {
        MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/v1/rides/ride-1/passengers/p1/accept");
        request1.addHeader("Idempotency-Key", "uuid-123");
        MockHttpServletResponse response1 = new MockHttpServletResponse();

        when(repository.save(any(IdempotencyKey.class))).thenAnswer(inv -> inv.getArgument(0));
        doAnswer(inv -> {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            var wrapper = (jakarta.servlet.http.HttpServletResponse) inv.getArgument(1);
            wrapper.setStatus(200);
            wrapper.setContentType("application/json");
            wrapper.getWriter().write("{\"success\":true,\"data\":{\"id\":\"ride-1\"}}");
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request1, response1, filterChain);

        assertThat(response1.getStatus()).isEqualTo(200);
        assertThat(response1.getContentAsString()).isEqualTo("{\"success\":true,\"data\":{\"id\":\"ride-1\"}}");
        verify(repository, times(2)).save(any(IdempotencyKey.class));
        verify(filterChain, times(1)).doFilter(any(), any());

        // Replay com a mesma chave
        IdempotencyKey completedEntry = IdempotencyKey.inProgress(
                "uuid-123", "user-1", "POST", "/api/v1/rides/ride-1/passengers/p1/accept");
        completedEntry.markCompleted(200, "application/json", "{\"success\":true,\"data\":{\"id\":\"ride-1\"}}");

        when(repository.save(any(IdempotencyKey.class))).thenThrow(new DuplicateKeyException("duplicate key"));
        when(repository.findByKey("uuid-123")).thenReturn(Optional.of(completedEntry));

        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/v1/rides/ride-1/passengers/p1/accept");
        request2.addHeader("Idempotency-Key", "uuid-123");
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        filter.doFilter(request2, response2, filterChain);

        assertThat(response2.getStatus()).isEqualTo(200);
        assertThat(response2.getContentAsString()).isEqualTo("{\"success\":true,\"data\":{\"id\":\"ride-1\"}}");
        // FilterChain não foi chamado novamente
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando requisição com mesma Idempotency-Key ainda está IN_PROGRESS")
    void shouldReturnConflictWhenInProgress() throws ServletException, IOException {
        IdempotencyKey inProgressEntry = IdempotencyKey.inProgress(
                "uuid-456", "user-1", "POST", "/api/v1/rides/request");
        when(repository.save(any(IdempotencyKey.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(repository.findByKey("uuid-456")).thenReturn(Optional.of(inProgressEntry));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/rides/request");
        request.addHeader("Idempotency-Key", "uuid-456");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("RIDE_CONFLICT");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando Idempotency-Key é reutilizada por outro usuário ou endpoint")
    void shouldReturnBadRequestWhenKeyUsedForDifferentPath() throws ServletException, IOException {
        IdempotencyKey existing = IdempotencyKey.inProgress(
                "uuid-789", "user-1", "POST", "/api/v1/rides/request");
        when(repository.save(any(IdempotencyKey.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(repository.findByKey("uuid-789")).thenReturn(Optional.of(existing));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/rides/ride-1/cancel");
        request.addHeader("Idempotency-Key", "uuid-789");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("BAD_REQUEST");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Deve remover chave de idempotência se ocorrer erro 500 para permitir retry")
    void shouldDeleteKeyOnServerError() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/rides/ride-1/cancel");
        request.addHeader("Idempotency-Key", "uuid-500");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(repository.save(any(IdempotencyKey.class))).thenAnswer(inv -> inv.getArgument(0));
        doAnswer(inv -> {
            var wrapper = (jakarta.servlet.http.HttpServletResponse) inv.getArgument(1);
            wrapper.setStatus(500);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        verify(repository).deleteByKey("uuid-500");
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
