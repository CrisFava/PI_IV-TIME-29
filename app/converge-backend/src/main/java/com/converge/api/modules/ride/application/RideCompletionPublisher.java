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
 *
 * A fonte de energia vem do que foi gravado na viagem ao criá-la; o cadastro atual do veículo
 * só é consultado para viagens antigas, criadas antes desse registro.
 */
@Component
@RequiredArgsConstructor
public class RideCompletionPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final RouteAccess routeAccess;
    private final VehicleAccess vehicleAccess;

    public void publish(Ride ride) {
        eventOf(ride).ifPresent(eventPublisher::publishEvent);
    }

    // O evento da viagem concluída, ou vazio se ela não conta para os indicadores.
    public Optional<RideCompletedEvent> eventOf(Ride ride) {
        if (ride.getStatus() != RideStatus.COMPLETED) {
            return Optional.empty();
        }

        List<String> passengerIds = ride.getParticipants().stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.COMPLETED)
                .map(Participant::getUserId)
                .toList();

        if (passengerIds.isEmpty()) {
            return Optional.empty();
        }

        RideCosts costs = ride.getCosts();
        boolean fuelRecorded = costs != null
                && costs.energySourceUsed() != null
                && costs.averageConsumptionUsed() != null;

        String energySource;
        Double averageConsumption;
        if (fuelRecorded) {
            energySource = costs.energySourceUsed().name();
            averageConsumption = costs.averageConsumptionUsed();
        } else {
            Optional<FuelSource> fuel = currentVehicleFuelOf(ride);
            energySource = fuel.map(source -> source.energySource().name()).orElse(null);
            averageConsumption = fuel.map(FuelSource::averageConsumption).orElse(null);
        }

        return Optional.of(new RideCompletedEvent(
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

    // Só para viagens antigas: fonte principal do veículo hoje; vazio se ele foi desativado.
    private Optional<FuelSource> currentVehicleFuelOf(Ride ride) {
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
