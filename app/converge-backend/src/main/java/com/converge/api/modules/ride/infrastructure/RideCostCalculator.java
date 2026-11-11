// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.infrastructure;

import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.RideCosts;
import com.converge.api.modules.route.application.RouteGeometryService;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.user.application.VehicleAccess;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.model.vehicle.subtypes.FuelSource;
import com.converge.api.shared.infrastructure.socket.SocketClient;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import com.converge.socket.core.protocol.Message;
import com.converge.socket.handler.ride.RideCostHandler;
import com.converge.socket.handler.ride.RideCostRequest;
import com.converge.socket.handler.ride.RideCostResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Adaptador de infraestrutura responsável por coletar dados de rota e veículo e delegar os cálculos de custo ao Socket Server.
 */
@Slf4j
@Component
public class RideCostCalculator {

    private final RouteGeometryService routeGeometryService;
    private final VehicleAccess vehicleAccess;
    private final String socketHost;
    private final int socketPort;

    @Autowired
    public RideCostCalculator(RouteGeometryService routeGeometryService, VehicleAccess vehicleAccess) {
        this(routeGeometryService, vehicleAccess, SocketClient.DEFAULT_HOST, SocketClient.DEFAULT_PORT);
    }

    RideCostCalculator(RouteGeometryService routeGeometryService, VehicleAccess vehicleAccess, String socketHost, int socketPort) {
        this.routeGeometryService = routeGeometryService;
        this.vehicleAccess = vehicleAccess;
        this.socketHost = socketHost;
        this.socketPort = socketPort;
    }

    public record ConfirmationQuote(double detourGeneratedKm, BigDecimal maxAmount) {
    }

    public ConfirmationQuote computeConfirmationQuote(Ride ride, String passengerId) {
        Participant passenger = findPassenger(ride, passengerId);
        FuelSource fuelSource = primaryFuelSource(ride);
        double baseDistanceKm = resolveBaseDistanceKm(ride);

        RideCostRequest request = buildConfirmationQuoteRequest(ride, passenger, fuelSource, baseDistanceKm);
        RideCostResponse response = executeOnServer(request);

        return new ConfirmationQuote(response.getDetourGeneratedKm(), response.getMaxAmount());
    }

    public void recalculateEstimatedShares(Ride ride) {
        List<Participant> confirmedPassengers = filterPassengersByStatus(ride, ParticipantStatus.CONFIRMED);
        FuelSource fuelSource = primaryFuelSource(ride);
        double baseDistanceKm = resolveBaseDistanceKm(ride);

        RideCostRequest request = buildCostSharingRequest(ride, confirmedPassengers, fuelSource, baseDistanceKm);
        RideCostResponse response = executeOnServer(request);

        RideCosts updatedCosts = buildRideCosts(ride.getCosts(), fuelSource, response);
        ride.applyCostEstimates(updatedCosts, response.getSharesByPassenger());
    }

    public void calculateFinalShares(Ride ride) {
        List<Participant> completedPassengers = filterPassengersByStatus(ride, ParticipantStatus.COMPLETED);
        if (completedPassengers.isEmpty()) {
            return;
        }

        FuelSource fuelSource = primaryFuelSource(ride);
        double baseDistanceKm = resolveBaseDistanceKm(ride);

        RideCostRequest request = buildCostSharingRequest(ride, completedPassengers, fuelSource, baseDistanceKm);
        RideCostResponse response = executeOnServer(request);

        RideCosts finalCosts = buildRideCosts(ride.getCosts(), fuelSource, response);
        ride.applyFinalAmounts(finalCosts, response.getSharesByPassenger());
    }

    private RideCostRequest buildConfirmationQuoteRequest(
            Ride ride,
            Participant passenger,
            FuelSource fuelSource,
            double baseDistanceKm
    ) {
        Place boarding = passenger.getBoardingLocation();
        Place dropOff = passenger.getDropOffLocation();

        Double leg1 = null;
        Double leg2 = null;
        Double leg3 = null;
        if (boarding != null && dropOff != null) {
            leg1 = routeGeometryService.compute(ride.getOrigin(), boarding).distanceKm();
            leg2 = routeGeometryService.compute(boarding, dropOff).distanceKm();
            leg3 = routeGeometryService.compute(dropOff, ride.getDestination()).distanceKm();
        }

        return RideCostRequest.forConfirmationQuote(
                baseDistanceKm,
                leg1,
                leg2,
                leg3,
                fuelSource.averageConsumption(),
                fuelSource.currentEnergyPrice(),
                resolveToll(ride.getCosts()),
                resolveParking(ride.getCosts())
        );
    }

    private RideCostRequest buildCostSharingRequest(
            Ride ride,
            List<Participant> passengers,
            FuelSource fuelSource,
            double baseDistanceKm
    ) {
        List<RideCostRequest.PassengerCostInput> passengerInputs = passengers.stream()
                .map(p -> new RideCostRequest.PassengerCostInput(p.getUserId(), p.getDetourGeneratedKm(), p.getMaxAmount()))
                .toList();

        return RideCostRequest.forCostSharing(
                baseDistanceKm,
                fuelSource.averageConsumption(),
                fuelSource.currentEnergyPrice(),
                resolveToll(ride.getCosts()),
                resolveParking(ride.getCosts()),
                passengerInputs
        );
    }

    private RideCostResponse executeOnServer(RideCostRequest request) {
        try (SocketClient client = new SocketClient(socketHost, socketPort)) {
            Message response = client.send(request);
            if (response instanceof RideCostResponse costResponse) {
                return costResponse;
            }
        } catch (Exception e) {
            log.warn("Socket Server indisponível para cálculo de custos ({}), executando handler local de contingência.", e.getMessage());
        }

        try {
            return new RideCostHandler().execute(request);
        } catch (Exception e) {
            throw new ApplicationException(
                    ErrorCode.SERVER_NOT_RUNNING,
                    "Erro no cálculo de custos da viagem: " + e.getMessage()
            );
        }
    }

    private Participant findPassenger(Ride ride, String passengerId) {
        return ride.getParticipants().stream()
                .filter(p -> p.getRole() == ParticipationRole.PASSENGER)
                .filter(p -> p.getUserId().equals(passengerId))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.BAD_REQUEST,
                        "Passageiro não encontrado nesta viagem.",
                        Set.of("passengerId")
                ));
    }

    private List<Participant> filterPassengersByStatus(Ride ride, ParticipantStatus status) {
        return ride.getParticipants().stream()
                .filter(p -> p.getRole() == ParticipationRole.PASSENGER)
                .filter(p -> p.getStatus() == status)
                .toList();
    }

    private FuelSource primaryFuelSource(Ride ride) {
        Vehicle vehicle = vehicleAccess.getActiveVehicleOf(ride.getVehicleId(), ride.getDriverId());
        return vehicle.getFuelSources().getFirst();
    }

    private double resolveBaseDistanceKm(Ride ride) {
        if (ride.getCosts() != null && ride.getCosts().routeDistanceKm() != null && ride.getCosts().routeDistanceKm() > 0) {
            return ride.getCosts().routeDistanceKm();
        }
        return routeGeometryService.compute(ride.getOrigin(), ride.getDestination()).distanceKm();
    }

    private BigDecimal resolveToll(RideCosts costs) {
        return costs != null && costs.toll() != null ? costs.toll() : BigDecimal.ZERO;
    }

    private BigDecimal resolveParking(RideCosts costs) {
        return costs != null && costs.parking() != null ? costs.parking() : BigDecimal.ZERO;
    }

    private RideCosts buildRideCosts(RideCosts currentCosts, FuelSource fuelSource, RideCostResponse response) {
        return new RideCosts(
                response.getBaseDistanceKm(),
                response.getTotalDistanceKm(),
                fuelSource.energySource(),
                fuelSource.averageConsumption(),
                fuelSource.currentEnergyPrice(),
                response.getEnergyCost(),
                resolveToll(currentCosts),
                resolveParking(currentCosts),
                response.getTotalCost(),
                response.getTotalShared()
        );
    }
}
