// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.user.application.RecordOverduePaymentUseCase;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterNonPaymentUseCaseTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RecordOverduePaymentUseCase recordOverduePayment;

    @InjectMocks
    private RegisterNonPaymentUseCase useCase;

    @Test
    @DisplayName("Quando a marcação acontece, deve registrar a dívida e devolver true")
    void shouldRecordDebtWhenMarked() {
        ObjectId rideId = new ObjectId();
        when(rideRepository.markNonPaymentAtomic(eq(rideId), eq("p1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(mock(Ride.class)));

        boolean registered = useCase.execute(rideId.toHexString(), "p1");

        assertThat(registered).isTrue();
        verify(recordOverduePayment).execute("p1");
    }

    @Test
    @DisplayName("Quando a cobrança já estava vencida ou não existe, não deve somar outra dívida")
    void shouldNotRecordDebtWhenNothingWasMarked() {
        ObjectId rideId = new ObjectId();
        when(rideRepository.markNonPaymentAtomic(eq(rideId), eq("p1"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        boolean registered = useCase.execute(rideId.toHexString(), "p1");

        assertThat(registered).isFalse();
        verify(recordOverduePayment, never()).execute(any());
    }

    @Test
    @DisplayName("Id de viagem inválido ou nulo deve devolver false sem consultar nada")
    void shouldIgnoreInvalidRideId() {
        assertThat(useCase.execute("invalido", "p1")).isFalse();
        assertThat(useCase.execute(null, "p1")).isFalse();

        verifyNoInteractions(rideRepository, recordOverduePayment);
    }
}
