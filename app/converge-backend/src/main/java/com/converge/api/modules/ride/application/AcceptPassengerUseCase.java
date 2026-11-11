// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.ride.infrastructure.RideCostCalculator;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.domain.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Caso de uso para o motorista aceitar a solicitação de vaga de um passageiro.
 */
@Service
@RequiredArgsConstructor
public class AcceptPassengerUseCase implements UseCase<PassengerRequestCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideEligibility rideEligibility;
    private final RideCostCalculator rideCostCalculator;
    private final RideRepository rideRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Ride execute(PassengerRequestCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        validateParticipantsEligibility(ride, command.passengerId());
        confirmPassengerWithQuote(ride, command.passengerId());
        rideCostCalculator.recalculateEstimatedShares(ride);

        Ride saved = rideRepository.save(ride);
        notifyConfirmedPassenger(command.passengerId(), command.rideId());

        return saved;
    }

    private void validateParticipantsEligibility(Ride ride, String passengerId) {
        rideEligibility.assertCanOffer(ride.getDriverId(), "Você");
        rideEligibility.assertCanJoin(passengerId, "O passageiro");
    }

    private void confirmPassengerWithQuote(Ride ride, String passengerId) {
        RideCostCalculator.ConfirmationQuote quote = rideCostCalculator.computeConfirmationQuote(ride, passengerId);
        Double detourKm = quote != null ? quote.detourGeneratedKm() : null;
        BigDecimal maxAmount = quote != null ? quote.maxAmount() : null;

        ride.acceptPassenger(passengerId, detourKm, maxAmount);
    }

    private void notifyConfirmedPassenger(String passengerId, String rideId) {
        eventPublisher.publishEvent(new RideNotificationEvent(
                List.of(passengerId),
                NotificationType.RIDE_CONFIRMED,
                "Carona confirmada",
                "O motorista aceitou sua solicitação de vaga.",
                rideId
        ));
    }
}
