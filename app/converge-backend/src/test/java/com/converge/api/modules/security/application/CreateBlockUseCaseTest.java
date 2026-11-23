// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application;

import com.converge.api.modules.security.application.gateway.UserLookupGateway;
import com.converge.api.modules.security.domain.model.Block;
import com.converge.api.modules.security.domain.repository.BlockRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link CreateBlockUseCase}.
 * Valida cenários como: Deve bloquear um usuário que existe e ainda não está bloqueado; Bloquear de novo devolve o bloqueio que já existe, sem salvar outro; Dois pedidos ao mesmo tempo: o índice único barra o segundo e devolve o primeiro.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
@ExtendWith(MockitoExtension.class)
class CreateBlockUseCaseTest {

    private static final String ME = "user-me";
    private static final String OTHER = "user-other";

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private UserLookupGateway userLookup;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateBlockUseCase createBlock() {
        return new CreateBlockUseCase(blockRepository, userLookup, eventPublisher);
    }

    @Test
    @DisplayName("Deve bloquear um usuário que existe e ainda não está bloqueado")
    void shouldCreateBlock() {
        when(userLookup.exists(OTHER)).thenReturn(true);
        when(blockRepository.findByUserIdAndBlockedUserId(ME, OTHER)).thenReturn(Optional.empty());
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Block block = createBlock().execute(new CreateBlockUseCase.Command(ME, OTHER));

        assertThat(block.getUserId()).isEqualTo(ME);
        assertThat(block.getBlockedUserId()).isEqualTo(OTHER);
        verify(blockRepository).save(any(Block.class));
    }

    @Test
    @DisplayName("Bloquear de novo devolve o bloqueio que já existe, sem salvar outro")
    void shouldBeIdempotentWhenBlockAlreadyExists() {
        Block existing = new Block(ME, OTHER);
        when(userLookup.exists(OTHER)).thenReturn(true);
        when(blockRepository.findByUserIdAndBlockedUserId(ME, OTHER)).thenReturn(Optional.of(existing));

        Block block = createBlock().execute(new CreateBlockUseCase.Command(ME, OTHER));

        assertThat(block).isSameAs(existing);
        verify(blockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dois pedidos ao mesmo tempo: o índice único barra o segundo e devolve o primeiro")
    void shouldReturnExistingBlockWhenConcurrentRequestWins() {
        Block winner = new Block(ME, OTHER);
        when(userLookup.exists(OTHER)).thenReturn(true);
        when(blockRepository.findByUserIdAndBlockedUserId(ME, OTHER))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(blockRepository.save(any(Block.class))).thenThrow(new DuplicateKeyException("duplicado"));

        Block block = createBlock().execute(new CreateBlockUseCase.Command(ME, OTHER));

        assertThat(block).isSameAs(winner);
    }

    @Test
    @DisplayName("Não deve bloquear a si mesmo")
    void shouldRejectSelfBlock() {
        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> createBlock().execute(new CreateBlockUseCase.Command(ME, ME)));

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        verifyNoInteractions(blockRepository);
    }

    @Test
    @DisplayName("Não deve bloquear usuário que não existe")
    void shouldRejectUnknownUser() {
        when(userLookup.exists(OTHER)).thenReturn(false);

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> createBlock().execute(new CreateBlockUseCase.Command(ME, OTHER)));

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        verifyNoInteractions(blockRepository);
    }

    @Test
    @DisplayName("Se o índice único barra o pedido e o bloqueio não aparece na releitura, repassa o erro original")
    void shouldRethrowDuplicateKeyWhenBlockIsNotFoundAfterwards() {
        DuplicateKeyException duplicate = new DuplicateKeyException("duplicado");
        when(userLookup.exists(OTHER)).thenReturn(true);
        when(blockRepository.findByUserIdAndBlockedUserId(ME, OTHER)).thenReturn(Optional.empty());
        when(blockRepository.save(any(Block.class))).thenThrow(duplicate);

        DuplicateKeyException thrown = assertThrows(DuplicateKeyException.class,
                () -> createBlock().execute(new CreateBlockUseCase.Command(ME, OTHER)));

        assertThat(thrown).isSameAs(duplicate);
    }
}
