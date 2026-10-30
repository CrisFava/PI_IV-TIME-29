package com.converge.api.modules.user.application.service;

import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserDeactivateTest {

    @Test
    @DisplayName("Deve desativar a conta do usuário")
    void shouldDeactivateUser() {
        User user = new User(
                "firebase-uid-789",
                "Carla Dias",
                "carla@puc-campinas.edu.br",
                "(19) 97777-6666",
                Set.of(ParticipationRole.PASSENGER),
                new AcademicRecord("PUC-Campinas", "Engenharia de Software", "2024-00789", "carla@puc-campinas.edu.br")
        );

        assertThat(user.isActive()).isTrue();    // 1. nasce ativo

        user.deactivate();                                  // 2. desativa

        assertThat(user.isActive()).isFalse();      // 3. agora está inativo
    }
}
