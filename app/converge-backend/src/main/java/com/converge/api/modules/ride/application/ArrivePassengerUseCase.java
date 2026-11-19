// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.ride.infrastructure.RideCostCalculator;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * "Confirmar chegada": o motorista encerra a viagem de um passageiro que desceu, com a viagem ainda
 * em andamento. O passageiro recebe o valor do rateio do momento e a cobrança já é emitida (RF39).
 */
@Service
@RequiredArgsConstructor
public class ArrivePassengerUseCase implements UseCase<PassengerRequestCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideCostCalculator rideCostCalculator;
    private final RideRepository rideRepository;
    private final RouteAccess routeAccess;
    private final RidePaymentNotifier paymentNotifier;

    @Override
    @Transactional(transactionManager = "mongoTransactionManager")
    public Ride execute(PassengerRequestCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        ride.arrivePassenger(command.passengerId());
        rideCostCalculator.calculateArrivalShare(ride, command.passengerId());
        List<String> charged = ride.chargeCompletedPassengers();

        Ride saved = rideRepository.save(ride);
        // A procura de um dia só de quem desceu acabou: ele deixa de aparecer como disponível no matching
        passengerRouteIdOf(saved, command.passengerId()).forEach(routeAccess::endIfOnce);
        paymentNotifier.notifyCharged(saved, charged);

        return saved;
    }

    private static List<String> passengerRouteIdOf(Ride ride, String passengerId) {
        return ride.getParticipants().stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getUserId().equals(passengerId))
                .filter(participant -> participant.getStatus() == ParticipantStatus.COMPLETED)
                .map(Participant::getPassengerRouteId)
                .filter(Objects::nonNull)
                .toList();
    }
}
