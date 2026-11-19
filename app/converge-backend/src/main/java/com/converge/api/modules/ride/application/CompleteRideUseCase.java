// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RideRefCommand;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.ride.infrastructure.RideCostCalculator;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Caso de uso para concluir uma viagem em andamento e consolidar o valor final dos participantes efetivos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompleteRideUseCase implements UseCase<RideRefCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideCostCalculator rideCostCalculator;
    private final RideRepository rideRepository;
    private final RideCompletionPublisher completionPublisher;
    private final RouteAccess routeAccess;

    // A viagem concluída e o fim da carona de um dia só são gravados juntos
    @Override
    @Transactional(transactionManager = "mongoTransactionManager")
    public Ride execute(RideRefCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        ride.complete();
        if (ride.getStatus() == RideStatus.COMPLETED) {
            rideCostCalculator.calculateFinalShares(ride);
        }

        Ride saved = rideRepository.save(ride);
        routeAccess.endIfOnce(saved.getDriverRouteId());
        // A procura de um dia só de quem estava na viagem também acabou: sem isso o passageiro
        // continuava aparecendo como disponível no matching dos motoristas.
        passengerRouteIdsOf(saved).forEach(routeAccess::endIfOnce);

        // A viagem já está gravada como concluída, que é o registro durável. Se os indicadores
        // falharem aqui, o CompletedRidesIndicatorReconciler reprocessa a viagem depois.
        try {
            completionPublisher.publish(saved);
        } catch (RuntimeException e) {
            log.warn("Indicadores da viagem {} ficam para o reprocessamento: {}", command.rideId(), e.getMessage());
        }

        return saved;
    }

    // Trajetos de procura de quem viajou ou faltou. Quem saiu ou foi recusado antes pode
    // continuar procurando outra carona com o mesmo trajeto.
    private static List<String> passengerRouteIdsOf(Ride ride) {
        return ride.getParticipants().stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.COMPLETED
                        || participant.getStatus() == ParticipantStatus.NO_SHOW)
                .map(Participant::getPassengerRouteId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
