// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.event.RideCompletedEvent;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.RideCosts;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.modules.route.domain.service.HaversineCalculator;
import com.converge.api.modules.user.application.VehicleAccess;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.model.vehicle.subtypes.FuelSource;
import com.converge.api.shared.presentation.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Avisa os outros módulos que uma viagem foi concluída, publicando o RideCompletedEvent.
 * Viagem que terminou sem passageiro efetivo vira CANCELLED e não gera evento (RF49).
 */
@Component
@RequiredArgsConstructor
public class RideCompletionPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final RouteAccess routeAccess;
    private final VehicleAccess vehicleAccess;

    public void publish(Ride ride) {
        if (ride.getStatus() != RideStatus.COMPLETED) {
            return;
        }

        List<String> passengerIds = ride.getParticipants().stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.COMPLETED)
                .map(Participant::getUserId)
                .toList();

        if (passengerIds.isEmpty()) {
            return;
        }

        Optional<FuelSource> fuel = fuelOf(ride);
        RideCosts costs = ride.getCosts();

        String energySource = costs != null && costs.energySourceUsed() != null
                ? costs.energySourceUsed().name()
                : fuel.map(source -> source.energySource().name()).orElse(null);

        Double averageConsumption = costs != null && costs.averageConsumptionUsed() != null
                ? costs.averageConsumptionUsed()
                : fuel.map(FuelSource::averageConsumption).orElse(null);

        eventPublisher.publishEvent(new RideCompletedEvent(
                ride.getId() != null ? ride.getId().toHexString() : null,
                ride.getDriverId(),
                passengerIds,
                distanceKmOf(ride),
                energySource,
                averageConsumption,
                ride.getCompletedAt() != null ? ride.getCompletedAt() : LocalDateTime.now()
        ));
    }

    // Distância já calculada na viagem; senão a do trajeto do motorista; em último caso, linha reta.
    private double distanceKmOf(Ride ride) {
        RideCosts costs = ride.getCosts();
        if (costs != null && costs.routeDistanceKm() != null) {
            return costs.routeDistanceKm();
        }

        try {
            Double routeDistanceKm = routeAccess
                    .getOwnedRoute(ride.getDriverRouteId(), ride.getDriverId())
                    .getRouteDistanceKm();

            if (routeDistanceKm != null) {
                return routeDistanceKm;
            }
        } catch (ApplicationException ignored) {
            // Trajeto excluído depois da viagem: cai no Haversine.
        }

        return HaversineCalculator.distanceKm(ride.getOrigin().coordinates(), ride.getDestination().coordinates());
    }

    // Fonte de energia principal do veículo; vazio se o veículo foi desativado depois da viagem.
    private Optional<FuelSource> fuelOf(Ride ride) {
        try {
            return vehicleAccess.getActiveVehicleOf(ride.getVehicleId(), ride.getDriverId())
                    .getFuelSources()
                    .stream()
                    .findFirst();
        } catch (ApplicationException ignored) {
            return Optional.empty();
        }
    }
}
