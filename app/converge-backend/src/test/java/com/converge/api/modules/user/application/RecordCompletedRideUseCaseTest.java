// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.UserReputationWriter;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.enums.user.VerificationMethod;
import com.converge.api.modules.user.domain.enums.user.VerificationStatus;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.domain.model.user.subtypes.AcademicRecord;
import com.converge.api.modules.user.domain.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordCompletedRideUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserReputationWriter reputationWriter;

    @InjectMocks
    private RecordCompletedRideUseCase useCase;

    private User validUser(String uid) {
        AcademicRecord academic = new AcademicRecord(
                "PUC-Campinas",
                "Engenharia de Software",
                "2024-00123",
                "ana" + uid + "@puc-campinas.edu.br",
                null,
                VerificationStatus.VERIFIED,
                VerificationMethod.INSTITUTIONAL_EMAIL,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1),
                null
        );
        User user = new User(
                "firebase-uid-" + uid,
                "Ana Souza",
                "ana" + uid + "@puc-campinas.edu.br",
                "(19) 91234-5678",
                Set.of(ParticipationRole.PASSENGER),
                academic
        );
        ReflectionTestUtils.setField(user, "id", new ObjectId());
        return user;
    }

    @Test
    @DisplayName("Deve contar a viagem para o motorista e para cada passageiro no papel certo")
    void shouldCountRideForDriverAndPassengers() {
        ObjectId driverId = new ObjectId();
        ObjectId passengerA = new ObjectId();
        ObjectId passengerB = new ObjectId();
        User driver = validUser("d");
        User a = validUser("a");
        User b = validUser("b");
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(userRepository.findById(passengerA)).thenReturn(Optional.of(a));
        when(userRepository.findById(passengerB)).thenReturn(Optional.of(b));

        useCase.execute(driverId.toHexString(), List.of(passengerA.toHexString(), passengerB.toHexString()));

        assertThat(driver.getReputation().driver().window().completedRides()).isEqualTo(1);
        assertThat(driver.getReputation().passenger().window().completedRides()).isZero();
        assertThat(a.getReputation().passenger().window().completedRides()).isEqualTo(1);
        assertThat(a.getReputation().driver().window().completedRides()).isZero();
        assertThat(b.getReputation().passenger().window().completedRides()).isEqualTo(1);
        verify(reputationWriter).saveDriverReputation(eq(driver.getId()), eq(driver.getReputation().driver()), any(LocalDateTime.class));
        verify(reputationWriter).savePassengerReputation(eq(a.getId()), eq(a.getReputation().passenger()), any(LocalDateTime.class));
        verify(reputationWriter).savePassengerReputation(eq(b.getId()), eq(b.getReputation().passenger()), any(LocalDateTime.class));
        verify(reputationWriter, never()).savePassengerReputation(eq(driver.getId()), any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve ignorar usuário inexistente e seguir com os demais")
    void shouldIgnoreMissingUserAndContinue() {
        ObjectId driverId = new ObjectId();
        ObjectId passengerId = new ObjectId();
        User passenger = validUser("p");
        when(userRepository.findById(driverId)).thenReturn(Optional.empty());
        when(userRepository.findById(passengerId)).thenReturn(Optional.of(passenger));

        useCase.execute(driverId.toHexString(), List.of(passengerId.toHexString()));

        assertThat(passenger.getReputation().passenger().window().completedRides()).isEqualTo(1);
        verify(reputationWriter, times(1)).savePassengerReputation(eq(passenger.getId()), eq(passenger.getReputation().passenger()), any(LocalDateTime.class));
        verify(reputationWriter, never()).saveDriverReputation(any(), any(), any());
    }

    @Test
    @DisplayName("Falha ao gravar um usuário não impede a contagem dos outros")
    void shouldKeepGoingWhenOneUserFails() {
        ObjectId driverId = new ObjectId();
        ObjectId passengerId = new ObjectId();
        User driver = validUser("d");
        User passenger = validUser("p");
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(userRepository.findById(passengerId)).thenReturn(Optional.of(passenger));
        when(reputationWriter.saveDriverReputation(eq(driver.getId()), any(), any()))
                .thenThrow(new IllegalStateException("banco fora do ar"));

        assertDoesNotThrow(() -> useCase.execute(driverId.toHexString(), List.of(passengerId.toHexString())));

        verify(reputationWriter).savePassengerReputation(eq(passenger.getId()), eq(passenger.getReputation().passenger()), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Deve ignorar ids nulos ou inválidos e lista de passageiros nula sem consultar o banco")
    void shouldIgnoreInvalidIds() {
        useCase.execute(null, null);
        useCase.execute("nao-e-object-id", List.of("outro-invalido"));

        verifyNoInteractions(userRepository, reputationWriter);
    }
}