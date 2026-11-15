// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.event.RideNotificationEvent;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.ride.infrastructure.RideCostCalculator;
import com.converge.api.shared.application.SafeActionHandler;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.domain.enums.NotificationType;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Caso de uso para o motorista aceitar a solicitação de vaga de um passageiro.
 */
@Slf4j
@Service
public class AcceptPassengerUseCase implements UseCase<PassengerRequestCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideEligibility rideEligibility;
    private final RideCostCalculator rideCostCalculator;
    private final RideItineraryService rideItineraryService;
    private final RideRepository rideRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Duration requestTimeout;

    public AcceptPassengerUseCase(
            RideAccess rideAccess,
            RideEligibility rideEligibility,
            RideCostCalculator rideCostCalculator,
            RideItineraryService rideItineraryService,
            RideRepository rideRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this(rideAccess, rideEligibility, rideCostCalculator, rideItineraryService, rideRepository, eventPublisher, Duration.ofMinutes(3));
    }

    public AcceptPassengerUseCase(
            RideAccess rideAccess,
            RideEligibility rideEligibility,
            RideCostCalculator rideCostCalculator,
            RideItineraryService rideItineraryService,
            RideRepository rideRepository,
            ApplicationEventPublisher eventPublisher,
            @Value("${converge.ride.request-timeout:3m}") Duration requestTimeout
    ) {
        this.rideAccess = rideAccess;
        this.rideEligibility = rideEligibility;
        this.rideCostCalculator = rideCostCalculator;
        this.rideItineraryService = rideItineraryService;
        this.rideRepository = rideRepository;
        this.eventPublisher = eventPublisher;
        this.requestTimeout = requestTimeout != null ? requestTimeout : Duration.ofMinutes(3);
    }

    @Override
    @Transactional
    public Ride execute(PassengerRequestCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        validateParticipantsEligibility(ride, command.passengerId());
        validateRequestNotExpired(ride, command.passengerId());

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
        // A rota passa a buscar e deixar o passageiro aceito
        rideItineraryService.refresh(updated);

        Ride saved = rideRepository.save(updated);
        notifyConfirmedPassenger(command.passengerId(), command.rideId());

        return saved;
    }

    private void validateParticipantsEligibility(Ride ride, String passengerId) {
        rideEligibility.assertCanOffer(ride.getDriverId(), "Você");
        rideEligibility.assertCanJoin(passengerId, "O passageiro");
    }

    private void validateRequestNotExpired(Ride ride, String passengerId) {
        Participant requestedPassenger = ride.getParticipants().stream()
                .filter(p -> p.getUserId().equals(passengerId) && p.getStatus() == ParticipantStatus.REQUESTED)
                .findFirst()
                .orElse(null);

        if (requestedPassenger != null
                && requestedPassenger.getRequestedAt().plus(this.requestTimeout).isBefore(LocalDateTime.now())) {
            ride.expireRequest(passengerId);
            rideRepository.save(ride);
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O tempo limite de resposta para esta solicitação já expirou.",
                    Set.of("passengerId")
            );
        }
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
