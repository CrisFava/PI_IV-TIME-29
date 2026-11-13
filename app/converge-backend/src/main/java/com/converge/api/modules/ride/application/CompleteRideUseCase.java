// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RideRefCommand;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.ride.infrastructure.RideCostCalculator;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    @Override
    public Ride execute(RideRefCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        ride.complete();
        if (ride.getStatus() == RideStatus.COMPLETED) {
            rideCostCalculator.calculateFinalShares(ride);
        }

        Ride saved = rideRepository.save(ride);

        // A viagem já está gravada como concluída, que é o registro durável. Se os indicadores
        // falharem aqui, o CompletedRidesIndicatorReconciler reprocessa a viagem depois.
        try {
            completionPublisher.publish(saved);
        } catch (RuntimeException e) {
            log.warn("Indicadores da viagem {} ficam para o reprocessamento: {}", command.rideId(), e.getMessage());
        }

        return saved;
    }
}
