// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RideHistoryItemDTO;
import com.converge.api.modules.ride.application.dto.RideHistoryQuery;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static com.converge.api.modules.ride.application.RideItineraryPlannerTest.place;
import static com.converge.api.modules.ride.application.RideItineraryPlannerTest.rideFromCenterToCampus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListRideHistoryUseCaseTest {

    private static final String UID = "firebase-ana";

    @Mock private RideAccess rideAccess;
    @Mock private RideRepository rideRepository;

    private ListRideHistoryUseCase useCase;
    private Ride completed;
    private Ride upcoming;

    @BeforeEach
    void setUp() {
        useCase = new ListRideHistoryUseCase(rideAccess, rideRepository);

        // Viagem concluída: Ana pagou R$ 6,00; Bruno foi recusado e não fez a viagem
        completed = rideFromCenterToCampus();
        completed.requestSeat("ana", "route-ana", place("Cambuí", -22.9000, -47.0600), place("Bloco A", -22.8400, -47.0530));
        completed.acceptPassenger("ana");
        completed.requestSeat("bruno", "route-bruno", place("Taquaral", -22.8800, -47.0570), place("Bloco B", -22.8350, -47.0520));
        completed.rejectPassenger("bruno", null);
        completed.start();
        completed.complete();
        var ana = completed.getParticipants().stream().filter(p -> "ana".equals(p.getUserId())).findFirst().orElseThrow();
        ReflectionTestUtils.setField(ana, "estimatedAmount", new BigDecimal("6.00"));
        // O valor pago é o cobrado pelo motorista e confirmado, não o calculado no fim
        ReflectionTestUtils.setField(ana, "finalAmount", new BigDecimal("5.10"));
        completed.requestPayment("ana", new BigDecimal("6.00"));
        completed.confirmPayment("ana");

        // Viagem que ainda vai acontecer: não é histórico
        upcoming = rideFromCenterToCampus();
        upcoming.requestSeat("ana", "route-ana-2", place("Cambuí", -22.9000, -47.0600), place("Bloco A", -22.8400, -47.0530));
    }

    private List<RideHistoryItemDTO> historyOf(String userId, RideHistoryQuery query) {
        when(rideAccess.currentUserId(UID)).thenReturn(userId);
        when(rideRepository.findByParticipantsUserId(userId)).thenReturn(List.of(completed, upcoming));
        return useCase.execute(query);
    }

    @Test
    @DisplayName("Passageiro vê o próprio trecho e o que pagou; motorista vê o custo e o que recebeu; só viagens terminadas")
    void shouldShowFinishedRidesFromTheViewerPointOfView() {
        RideHistoryItemDTO asPassenger = historyOf("ana", new RideHistoryQuery(UID, null, null, null)).getFirst();
        assertThat(asPassenger.role()).isEqualTo(ParticipationRole.PASSENGER);
        assertThat(asPassenger.origin().address()).isEqualTo("Cambuí");
        assertThat(asPassenger.paidAmount()).isEqualByComparingTo("6.00");
        assertThat(asPassenger.people()).isEqualTo(2);

        List<RideHistoryItemDTO> asDriver = historyOf("driver-1", new RideHistoryQuery(UID, null, null, null));
        assertThat(asDriver).hasSize(1);
        assertThat(asDriver.getFirst().role()).isEqualTo(ParticipationRole.DRIVER);
        assertThat(asDriver.getFirst().paidAmount()).isEqualByComparingTo("6.00");

        // Bruno foi recusado: a viagem não aparece no histórico dele
        assertThat(historyOf("bruno", new RideHistoryQuery(UID, null, null, null))).isEmpty();
    }
}
