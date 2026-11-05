package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.RequestRideSeatCommand;
import com.converge.api.modules.ride.application.mapper.RideMapper;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.route.application.RouteAccess;
import com.converge.api.modules.route.domain.enums.RouteStatus;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.modules.user.domain.repository.VehicleRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RequestRideSeatUseCase implements UseCase<RequestRideSeatCommand, Ride> {

    private final RideAccess rideAccess;
    private final RideEligibility rideEligibility;
    private final RouteAccess routeAccess;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final RideRepository rideRepository;
    private final RideMapper rideMapper;

    @Override
    public Ride execute(RequestRideSeatCommand command) {
        String passengerId = rideAccess.currentUserId(command.firebaseUid());
        rideEligibility.assertCanJoin(passengerId, "Você");

        LocalDate date = command.request().date();

        Route passengerRoute = routeAccess.getOwnedRoute(command.request().passengerRouteId(), passengerId);
        validatePassengerRoute(passengerRoute, date);

        Route driverRoute = loadDriverRoute(command.request().driverRouteId());
        validateDriverRoute(driverRoute, passengerId, date);

        LocalDateTime departure = date.atTime(LocalTime.parse(driverRoute.getDepartureTime()));

        Ride ride = rideRepository
                .findByDriverRouteIdAndDepartureDateTimeAndStatusNot(
                        driverRoute.getId().toHexString(), departure, RideStatus.CANCELLED)
                .orElseGet(() -> createRide(driverRoute, date, departure));

        String passengerRouteId = passengerRoute.getId().toHexString();

        ride.requestSeat(passengerId, passengerRouteId, passengerRoute.getOrigin(), passengerRoute.getDestination());

        try {
            return rideRepository.save(ride);
        } catch (DuplicateKeyException exception) {
            if (!isDuplicatedOpenRide(exception)) {
                throw rideConflict();
            }

            Ride existing = rideRepository
                    .findByDriverRouteIdAndDepartureDateTimeAndStatusNot(
                            driverRoute.getId().toHexString(), departure, RideStatus.CANCELLED)
                    .orElseThrow(this::rideConflict);

            existing.requestSeat(
                    passengerId, passengerRouteId, passengerRoute.getOrigin(), passengerRoute.getDestination());

            return rideRepository.save(existing);
        }
    }

    private boolean isDuplicatedOpenRide(DuplicateKeyException exception) {
        String message = exception.getMostSpecificCause().getMessage();

        return (message != null && message.contains(Ride.UNIQUE_OPEN_RIDE_INDEX))
                || (exception.getMessage() != null && exception.getMessage().contains(Ride.UNIQUE_OPEN_RIDE_INDEX));
    }

    private ApplicationException rideConflict() {
        return new ApplicationException(
                ErrorCode.RIDE_CONFLICT,
                "Não foi possível registrar o pedido agora. Tente novamente."
        );
    }

    private void validatePassengerRoute(Route route, LocalDate date) {
        if (route.getType() != RouteType.SEARCH || route.getStatus() != RouteStatus.ACTIVE) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Só é possível pedir vaga com um trajeto de procura que esteja ativo.",
                    Set.of("passengerRouteId")
            );
        }

        if (!route.getWeekDays().contains(date.getDayOfWeek())) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "A data escolhida não cai em nenhum dos dias da semana do seu trajeto.",
                    Set.of("date")
            );
        }
    }

    private Route loadDriverRoute(String routeId) {
        if (!ObjectId.isValid(routeId)) {
            throw driverRouteNotFound();
        }

        return routeRepository.findById(new ObjectId(routeId))
                .filter(route -> route.getStatus() == RouteStatus.ACTIVE)
                .orElseThrow(this::driverRouteNotFound);
    }

    private void validateDriverRoute(Route route, String passengerId, LocalDate date) {
        if (route.getType() != RouteType.OFFER) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O trajeto escolhido não é uma oferta de carona.",
                    Set.of("driverRouteId")
            );
        }

        if (!route.getWeekDays().contains(date.getDayOfWeek())) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "A data escolhida não cai em nenhum dos dias da semana do trajeto do motorista.",
                    Set.of("date")
            );
        }

        if (route.isOwnedBy(passengerId)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Você não pode pedir vaga no seu próprio trajeto.",
                    Set.of("driverRouteId")
            );
        }

        if (route.getTotalSeats() == null) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "O trajeto do motorista não tem o número de vagas definido.",
                    Set.of("driverRouteId")
            );
        }
    }

    private Ride createRide(Route driverRoute, LocalDate date, LocalDateTime departure) {
        String driverId = driverRoute.getUserId();
        rideEligibility.assertCanOffer(driverId, "O motorista");

        Vehicle vehicle = loadVehicle(driverRoute, driverId);

        LocalDateTime arrival = date.atTime(LocalTime.parse(driverRoute.getEstimatedArrivalTime()));
        if (arrival.isBefore(departure)) {
            arrival = arrival.plusDays(1);
        }

        return new Ride(
                nextSequentialNumber(driverId),
                driverId,
                vehicle.getId().toHexString(),
                rideMapper.toVehicleSnapshot(vehicle),
                driverRoute.getId().toHexString(),
                driverRoute.getOrigin(),
                driverRoute.getDestination(),
                departure,
                arrival,
                driverRoute.getWeekDays(),
                driverRoute.getTotalSeats()
        );
    }

    private Vehicle loadVehicle(Route route, String driverId) {
        String vehicleId = route.getVehicleId();

        if (vehicleId == null || !ObjectId.isValid(vehicleId)) {
            throw vehicleNotFound();
        }

        return vehicleRepository.findById(new ObjectId(vehicleId))
                .filter(Vehicle::isActive)
                .filter(vehicle -> vehicle.isOwnedBy(driverId))
                .orElseThrow(this::vehicleNotFound);
    }

    private int nextSequentialNumber(String driverId) {
        return rideRepository.findTopByDriverIdOrderBySequentialNumberDesc(driverId)
                .map(ride -> ride.getSequentialNumber() + 1)
                .orElse(1);
    }

    private ApplicationException driverRouteNotFound() {
        return new ApplicationException(ErrorCode.ROUTE_NOT_FOUND, "Trajeto do motorista não encontrado.");
    }

    private ApplicationException vehicleNotFound() {
        return new ApplicationException(ErrorCode.VEHICLE_NOT_FOUND, "O veículo do trajeto não foi encontrado.");
    }
}
