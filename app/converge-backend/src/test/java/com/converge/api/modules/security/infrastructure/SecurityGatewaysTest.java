// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.infrastructure;

import com.converge.api.modules.ride.application.GetRideParticipantsUseCase;
import com.converge.api.modules.ride.application.dto.RideParticipantsDTO;
import com.converge.api.modules.security.application.gateway.RideParticipationLookupGateway.RideParticipants;
import com.converge.api.modules.user.application.GetUserEligibilityUseCase;
import com.converge.api.modules.user.application.GetUserNamesUseCase;
import com.converge.api.modules.user.application.dto.UserEligibilityDTO;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityGatewaysTest {

    @Mock
    private GetUserEligibilityUseCase getUserEligibility;

    @Mock
    private GetUserNamesUseCase getUserNames;

    @Mock
    private GetRideParticipantsUseCase getRideParticipants;

    @Test
    @DisplayName("Usuário que o módulo de usuários encontra existe")
    void shouldSayUserExists() {
        when(getUserEligibility.execute("u1")).thenReturn(new UserEligibilityDTO(true, true, false, false, false));

        assertThat(new UserModuleLookupGateway(getUserEligibility, getUserNames).exists("u1")).isTrue();
    }

    @Test
    @DisplayName("USER_NOT_FOUND do módulo de usuários vira 'não existe'")
    void shouldSayUserDoesNotExist() {
        when(getUserEligibility.execute("u1"))
                .thenThrow(new ApplicationException(ErrorCode.USER_NOT_FOUND, "Usuário não encontrado."));

        assertThat(new UserModuleLookupGateway(getUserEligibility, getUserNames).exists("u1")).isFalse();
    }

    @Test
    @DisplayName("Outros erros do módulo de usuários continuam sendo lançados")
    void shouldRethrowOtherErrors() {
        when(getUserEligibility.execute("u1"))
                .thenThrow(new ApplicationException(ErrorCode.EXTERNAL_SERVICE_ERROR, "Falha."));
        UserModuleLookupGateway gateway = new UserModuleLookupGateway(getUserEligibility, getUserNames);

        assertThrows(ApplicationException.class, () -> gateway.exists("u1"));
    }

    @Test
    @DisplayName("Os nomes vêm do caso de uso de leitura do módulo de usuários")
    void shouldDelegateNames() {
        when(getUserNames.execute(List.of("u1"))).thenReturn(Map.of("u1", "Ana"));

        assertThat(new UserModuleLookupGateway(getUserEligibility, getUserNames).namesOf(List.of("u1")))
                .containsEntry("u1", "Ana");
    }

    @Test
    @DisplayName("Deve traduzir a viagem do módulo ride em motorista e passageiros")
    void shouldMapRideParticipants() {
        when(getRideParticipants.execute("ride-1")).thenReturn(Optional.of(
                new RideParticipantsDTO("ride-1", "driver-1", List.of("p1", "p2"))));

        Optional<RideParticipants> ride =
                new RideModuleParticipationLookupGateway(getRideParticipants).findParticipants("ride-1");

        assertThat(ride).contains(new RideParticipants("ride-1", "driver-1", Set.of("p1", "p2")));
        assertThat(ride.get().participated("driver-1")).isTrue();
        assertThat(ride.get().participated("p2")).isTrue();
        assertThat(ride.get().participated("outro")).isFalse();
        assertThat(ride.get().participated(null)).isFalse();
    }

    @Test
    @DisplayName("Viagem inexistente continua vazia")
    void shouldReturnEmptyWhenRideDoesNotExist() {
        when(getRideParticipants.execute("ride-1")).thenReturn(Optional.empty());

        assertThat(new RideModuleParticipationLookupGateway(getRideParticipants).findParticipants("ride-1")).isEmpty();
    }
}
