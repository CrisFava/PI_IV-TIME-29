// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application;

import com.converge.api.modules.security.domain.repository.BlockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteBlockUseCaseTest {

    private static final String ME = "user-me";
    private static final String OTHER = "user-other";

    @Mock
    private BlockRepository blockRepository;

    @Test
    @DisplayName("Desbloquear remove só o bloqueio feito por quem pede")
    void shouldDeleteOnlyOwnBlock() {
        new DeleteBlockUseCase(blockRepository).execute(new DeleteBlockUseCase.Command(ME, OTHER));

        verify(blockRepository).deleteByUserIdAndBlockedUserId(ME, OTHER);
    }

    @Test
    @DisplayName("Desbloquear sem bloqueio existente não dá erro")
    void shouldNotFailWhenThereIsNothingToDelete() {
        when(blockRepository.deleteByUserIdAndBlockedUserId(ME, OTHER)).thenReturn(0L);

        new DeleteBlockUseCase(blockRepository).execute(new DeleteBlockUseCase.Command(ME, OTHER));

        verify(blockRepository).deleteByUserIdAndBlockedUserId(ME, OTHER);
    }
}