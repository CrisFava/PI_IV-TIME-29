// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.ride.application;

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
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Serviço de aplicação responsável pelo cálculo de desvio de rota, valor máximo (teto) e rateio de custos da viagem.
 */
@Service
@RequiredArgsConstructor
public class RideCostCalculator {

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private final RouteGeometryService routeGeometryService;
    private final VehicleAccess vehicleAccess;

    public record ConfirmationQuote(double detourGeneratedKm, BigDecimal maxAmount) {
    }

    public ConfirmationQuote computeConfirmationQuote(Ride ride, String passengerId) {
        Participant passenger = findPassenger(ride, passengerId);
        FuelSource fuelSource = primaryFuelSource(ride);

        double baseDistanceKm = resolveBaseDistanceKm(ride);
        double detourKm = computePassengerDetourKm(
                baseDistanceKm,
                ride.getOrigin(),
                ride.getDestination(),
                passenger.getBoardingLocation(),
                passenger.getDropOffLocation()
        );

        BigDecimal energyCostWithPassenger = computeEnergyCost(baseDistanceKm + detourKm, fuelSource);
        BigDecimal totalWithPassenger = computeTotalTripCost(energyCostWithPassenger, ride.getCosts());
        BigDecimal maxAmount = totalWithPassenger.divide(TWO, 2, RoundingMode.HALF_EVEN);

        return new ConfirmationQuote(detourKm, maxAmount);
    }

    public void recalculateEstimatedShares(Ride ride) {
        List<Participant> confirmedPassengers = filterPassengersByStatus(ride, ParticipantStatus.CONFIRMED);
        FuelSource fuelSource = primaryFuelSource(ride);

        double baseDistanceKm = resolveBaseDistanceKm(ride);
        double totalDetoursKm = sumDetoursKm(confirmedPassengers);
        double totalDistanceKm = roundKm(baseDistanceKm + totalDetoursKm);

        BigDecimal energyCost = computeEnergyCost(totalDistanceKm, fuelSource);
        BigDecimal totalCost = computeTotalTripCost(energyCost, ride.getCosts());
        Map<String, BigDecimal> sharesByPassenger = computeShares(totalCost, confirmedPassengers);
        BigDecimal totalShared = sumShares(sharesByPassenger);

        RideCosts updatedCosts = buildRideCosts(ride.getCosts(), baseDistanceKm, totalDistanceKm, fuelSource, energyCost, totalCost, totalShared);
        ride.applyCostEstimates(updatedCosts, sharesByPassenger);
    }

    public void calculateFinalShares(Ride ride) {
        List<Participant> completedPassengers = filterPassengersByStatus(ride, ParticipantStatus.COMPLETED);
        if (completedPassengers.isEmpty()) {
            return;
        }

        FuelSource fuelSource = primaryFuelSource(ride);
        double baseDistanceKm = resolveBaseDistanceKm(ride);
        double totalDetoursKm = sumDetoursKm(completedPassengers);
        double totalDistanceKm = roundKm(baseDistanceKm + totalDetoursKm);

        BigDecimal energyCost = computeEnergyCost(totalDistanceKm, fuelSource);
        BigDecimal totalCost = computeTotalTripCost(energyCost, ride.getCosts());
        Map<String, BigDecimal> finalSharesByPassenger = computeShares(totalCost, completedPassengers);
        BigDecimal totalShared = sumShares(finalSharesByPassenger);

        RideCosts finalCosts = buildRideCosts(ride.getCosts(), baseDistanceKm, totalDistanceKm, fuelSource, energyCost, totalCost, totalShared);
        ride.applyFinalAmounts(finalCosts, finalSharesByPassenger);
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
        return roundKm(routeGeometryService.compute(ride.getOrigin(), ride.getDestination()).distanceKm());
    }

    private double computePassengerDetourKm(
            double baseDistanceKm,
            Place driverOrigin,
            Place driverDestination,
            Place boarding,
            Place dropOff
    ) {
        if (boarding == null || dropOff == null) {
            return 0.0;
        }
        double leg1 = routeGeometryService.compute(driverOrigin, boarding).distanceKm();
        double leg2 = routeGeometryService.compute(boarding, dropOff).distanceKm();
        double leg3 = routeGeometryService.compute(dropOff, driverDestination).distanceKm();
        double rawDetour = (leg1 + leg2 + leg3) - baseDistanceKm;
        return Math.max(0.0, roundKm(rawDetour));
    }

    private double sumDetoursKm(List<Participant> passengers) {
        return passengers.stream()
                .map(Participant::getDetourGeneratedKm)
                .filter(d -> d != null && d > 0)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private BigDecimal computeEnergyCost(double distanceKm, FuelSource fuelSource) {
        BigDecimal distance = BigDecimal.valueOf(distanceKm);
        BigDecimal consumption = BigDecimal.valueOf(fuelSource.averageConsumption());
        BigDecimal unitsConsumed = distance.divide(consumption, 6, RoundingMode.HALF_EVEN);
        return unitsConsumed.multiply(fuelSource.currentEnergyPrice()).setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal computeTotalTripCost(BigDecimal energyCost, RideCosts currentCosts) {
        BigDecimal toll = currentCosts != null && currentCosts.toll() != null ? currentCosts.toll() : BigDecimal.ZERO;
        BigDecimal parking = currentCosts != null && currentCosts.parking() != null ? currentCosts.parking() : BigDecimal.ZERO;
        return energyCost.add(toll).add(parking).setScale(2, RoundingMode.HALF_EVEN);
    }

    private Map<String, BigDecimal> computeShares(BigDecimal totalCost, List<Participant> passengers) {
        Map<String, BigDecimal> shares = new HashMap<>();
        if (passengers.isEmpty()) {
            return shares;
        }

        BigDecimal divisor = BigDecimal.valueOf(1L + passengers.size());
        BigDecimal equalShare = totalCost.divide(divisor, 2, RoundingMode.HALF_EVEN);

        for (Participant passenger : passengers) {
            BigDecimal maxAmount = passenger.getMaxAmount();
            BigDecimal capped = (maxAmount != null && maxAmount.signum() > 0 && equalShare.compareTo(maxAmount) > 0)
                    ? maxAmount
                    : equalShare;
            shares.put(passenger.getUserId(), capped);
        }

        return shares;
    }

    private BigDecimal sumShares(Map<String, BigDecimal> shares) {
        return shares.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private RideCosts buildRideCosts(
            RideCosts currentCosts,
            double baseDistanceKm,
            double totalDistanceKm,
            FuelSource fuelSource,
            BigDecimal energyCost,
            BigDecimal totalCost,
            BigDecimal totalShared
    ) {
        BigDecimal toll = currentCosts != null && currentCosts.toll() != null ? currentCosts.toll() : BigDecimal.ZERO;
        BigDecimal parking = currentCosts != null && currentCosts.parking() != null ? currentCosts.parking() : BigDecimal.ZERO;

        return new RideCosts(
                baseDistanceKm,
                totalDistanceKm,
                fuelSource.energySource(),
                fuelSource.averageConsumption(),
                fuelSource.currentEnergyPrice(),
                energyCost,
                toll,
                parking,
                totalCost,
                totalShared
        );
    }

    private double roundKm(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
