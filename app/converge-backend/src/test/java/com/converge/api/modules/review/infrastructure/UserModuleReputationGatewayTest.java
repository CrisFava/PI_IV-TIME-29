// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.infrastructure;

import com.converge.api.modules.review.domain.enums.ReviewedRole;
import com.converge.api.modules.user.application.RecordReviewRatingUseCase;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * Testes unitários para {@link UserModuleReputationGateway}.
 * Valida cenários como: Avaliação do motorista vai para o papel DRIVER do usuário; Avaliação do passageiro vai para o papel PASSENGER do usuário.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
@ExtendWith(MockitoExtension.class)
class UserModuleReputationGatewayTest {

    @Mock
    private RecordReviewRatingUseCase recordReviewRating;

    @InjectMocks
    private UserModuleReputationGateway gateway;

    @Test
    @DisplayName("Avaliação do motorista vai para o papel DRIVER do usuário")
    void shouldMapDriverRole() {
        gateway.updateRating("u1", ReviewedRole.DRIVER, 4.5, 2);

        verify(recordReviewRating).execute("u1", ParticipationRole.DRIVER, 4.5, 2);
    }

    @Test
    @DisplayName("Avaliação do passageiro vai para o papel PASSENGER do usuário")
    void shouldMapPassengerRole() {
        gateway.updateRating("u2", ReviewedRole.PASSENGER, 3.0, 1);

        verify(recordReviewRating).execute("u2", ParticipationRole.PASSENGER, 3.0, 1);
    }
}
