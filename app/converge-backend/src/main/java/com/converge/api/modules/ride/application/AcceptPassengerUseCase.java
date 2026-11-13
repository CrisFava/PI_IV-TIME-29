// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.ride.infrastructure.RideCostCalculator;
import com.converge.api.shared.application.SafeActionHandler;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.domain.enums.NotificationType;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso para o motorista aceitar a solicitação de vaga de um passageiro.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcceptPassengerUseCase implements UseCase<PassengerRequestCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideEligibility rideEligibility;
    private final RideCostCalculator rideCostCalculator;
    private final RideRepository rideRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Ride execute(PassengerRequestCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        validateParticipantsEligibility(ride, command.passengerId());

        RideCostCalculator.ConfirmationQuote quote = rideCostCalculator.computeConfirmationQuote(ride, command.passengerId());
        Double detourKm = quote != null ? quote.detourGeneratedKm() : null;
        BigDecimal maxAmount = quote != null ? quote.maxAmount() : null;

        Ride updated = rideRepository.acceptPassengerAtomic(
                        ride.getId(),
                        command.passengerId(),
                        detourKm,
                        maxAmount,
                        LocalDateTime.now()
                )
                .orElseThrow(() -> resolveAcceptConflict(ride, command.passengerId(), detourKm, maxAmount));

        rideCostCalculator.recalculateEstimatedShares(updated);

        Ride saved = rideRepository.save(updated);
        notifyConfirmedPassenger(command.passengerId(), command.rideId());

        return saved;
    }

    private void validateParticipantsEligibility(Ride ride, String passengerId) {
        rideEligibility.assertCanOffer(ride.getDriverId(), "Você");
        rideEligibility.assertCanJoin(passengerId, "O passageiro");
    }

    private ApplicationException resolveAcceptConflict(
            Ride fallbackRide,
            String passengerId,
            Double detourKm,
            BigDecimal maxAmount
    ) {
        Ride latest = fallbackRide.getId() != null
                ? rideRepository.findById(fallbackRide.getId()).orElse(fallbackRide)
                : fallbackRide;
        latest.acceptPassenger(passengerId, detourKm, maxAmount);
        return new ApplicationException(
                ErrorCode.RIDE_CONFLICT,
                "Não foi possível aceitar o passageiro agora. Tente novamente."
        );
    }

    private void notifyConfirmedPassenger(String passengerId, String rideId) {
        SafeActionHandler.run(log, "publicar evento de confirmação de carona", () ->
                eventPublisher.publishEvent(new RideNotificationEvent(
                        List.of(passengerId),
                        NotificationType.RIDE_CONFIRMED,
                        "Carona confirmada",
                        "O motorista aceitou sua solicitação de vaga.",
                        rideId
                ))
        );
    }
}
