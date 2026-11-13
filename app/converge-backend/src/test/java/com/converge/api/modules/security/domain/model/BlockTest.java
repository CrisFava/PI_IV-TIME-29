// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.security.domain.model;

import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlockTest {

    @Test
    @DisplayName("Guarda quem bloqueou e quem foi bloqueado")
    void shouldKeepBothSides() {
        Block block = new Block("user-a", "user-b");

        assertThat(block.getUserId()).isEqualTo("user-a");
        assertThat(block.getBlockedUserId()).isEqualTo("user-b");
    }

    @Test
    @DisplayName("Não deixa o usuário bloquear a si mesmo")
    void shouldRejectSelfBlock() {
        ApplicationException ex = assertThrows(ApplicationException.class, () -> new Block("user-a", "user-a"));

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ex.getError().fields()).containsExactly("blockedUserId");
    }

    @Test
    @DisplayName("Exige os dois usuários")
    void shouldRejectMissingUsers() {
        assertThrows(ApplicationException.class, () -> new Block(null, "user-b"));
        assertThrows(ApplicationException.class, () -> new Block("user-a", " "));
    }
}
