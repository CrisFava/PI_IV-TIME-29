// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.security.application;

import com.converge.api.modules.security.domain.model.Block;
import com.converge.api.modules.security.domain.repository.BlockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockAccessTest {

    @Mock
    private BlockRepository blockRepository;

    @InjectMocks
    private BlockAccess blockAccess;

    @Test
    @DisplayName("O bloqueio vale nos dois sentidos: quem o usuário bloqueou e quem o bloqueou (RF47)")
    void shouldReturnBothDirections() {
        when(blockRepository.findByUserIdOrBlockedUserId("me", "me")).thenReturn(List.of(
                new Block("me", "blocked-by-me"),
                new Block("blocked-me", "me")
        ));

        assertThat(blockAccess.blockedUserIdsOf("me")).containsExactlyInAnyOrder("blocked-by-me", "blocked-me");
    }

    @Test
    @DisplayName("Sem usuário não há bloqueios e o banco nem é consultado")
    void shouldReturnEmptyWithoutUser() {
        assertThat(blockAccess.blockedUserIdsOf(" ")).isEmpty();
        assertThat(blockAccess.blockedUserIdsOf(null)).isEmpty();
        verifyNoInteractions(blockRepository);
    }
}
