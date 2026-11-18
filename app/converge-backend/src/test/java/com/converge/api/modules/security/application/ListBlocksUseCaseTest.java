// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application;

import com.converge.api.modules.security.application.dto.BlockResponseDTO;
import com.converge.api.modules.security.application.gateway.UserLookupGateway;
import com.converge.api.modules.security.domain.model.Block;
import com.converge.api.modules.security.domain.repository.BlockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListBlocksUseCaseTest {

    private static final String ME = "user-me";

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private UserLookupGateway userLookup;

    @Test
    @DisplayName("Deve listar os bloqueados com o nome de cada um")
    void shouldListBlocksWithNames() {
        when(blockRepository.findByUserIdOrderByCreatedAtDesc(ME))
                .thenReturn(List.of(new Block(ME, "u1"), new Block(ME, "u2")));
        when(userLookup.namesOf(List.of("u1", "u2"))).thenReturn(Map.of("u1", "Ana"));

        List<BlockResponseDTO> blocks = new ListBlocksUseCase(blockRepository, userLookup).execute(ME);

        assertThat(blocks).extracting(BlockResponseDTO::blockedUserId).containsExactly("u1", "u2");
        assertThat(blocks).extracting(BlockResponseDTO::blockedUserName).containsExactly("Ana", null);
    }

    @Test
    @DisplayName("Sem bloqueios devolve lista vazia sem buscar nomes")
    void shouldReturnEmptyListWhenThereAreNoBlocks() {
        when(blockRepository.findByUserIdOrderByCreatedAtDesc(ME)).thenReturn(List.of());

        assertThat(new ListBlocksUseCase(blockRepository, userLookup).execute(ME)).isEmpty();

        verifyNoInteractions(userLookup);
    }
}