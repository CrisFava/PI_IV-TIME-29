// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.dto.RidePreviewResponseDTO;
import com.converge.api.modules.ride.application.mapper.RideMapper;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.RideStop;
import com.converge.api.modules.route.application.RouteGeometryService;
import com.converge.api.modules.route.application.gateway.RouteGeometry;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Antes de responder a um pedido de vaga, o motorista vê como ficaria a viagem:
 * o caminho de agora, o caminho buscando e deixando o passageiro e o desvio que ele
 * acrescenta. Nada é gravado.
 */
@Service
public class PreviewPassengerRideUseCase implements UseCase<PassengerRequestCommand, RidePreviewResponseDTO> {

    private final RideAccess rideAccess;
    private final RideItineraryPlanner planner;
    private final RouteGeometryService routeGeometry;
    private final RideMapper rideMapper;
    private final Duration requestTimeout;

    public PreviewPassengerRideUseCase(
            RideAccess rideAccess,
            RideItineraryPlanner planner,
            RouteGeometryService routeGeometry,
            RideMapper rideMapper
    ) {
        this(rideAccess, planner, routeGeometry, rideMapper, Duration.ofHours(1));
    }

    @Autowired
    public PreviewPassengerRideUseCase(
            RideAccess rideAccess,
            RideItineraryPlanner planner,
            RouteGeometryService routeGeometry,
            RideMapper rideMapper,
            @Value("${converge.ride.request-timeout:1h}") Duration requestTimeout
    ) {
        this.rideAccess = rideAccess;
        this.planner = planner;
        this.routeGeometry = routeGeometry;
        this.rideMapper = rideMapper;
        this.requestTimeout = requestTimeout != null ? requestTimeout : Duration.ofHours(1);
    }

    @Override
    public RidePreviewResponseDTO execute(PassengerRequestCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        // O mesmo que o aceite exigiria: pedido dentro do prazo e viagem antes do corte
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime requestCutoff = now.minus(this.requestTimeout);
        boolean pending = ride.getParticipants().stream()
                .anyMatch(participant -> participant.getRole() == ParticipationRole.PASSENGER
                        && participant.getUserId().equals(command.passengerId())
                        && participant.getStatus() == ParticipantStatus.REQUESTED
                        && (participant.getRequestedAt() == null
                                || participant.getRequestedAt().isAfter(requestCutoff)));
        boolean beforeCutoff = ride.getDepartureDateTime() != null
                && ride.getDepartureDateTime().isAfter(now.plusMinutes(Ride.REQUEST_CUTOFF_MINUTES));
        if (!pending || !beforeCutoff) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Não há pedido de vaga deste passageiro esperando resposta.",
                    Set.of("passengerId")
            );
        }

        RouteGeometry current = geometryOf(ride, planner.plan(ride));
        List<RideStop> stopsWithPassenger = planner.plan(ride, command.passengerId());
        RouteGeometry withPassenger = geometryOf(ride, stopsWithPassenger);

        // Um caminho do Google e outro estimado não se comparam: sem desvio nesse caso
        boolean comparable = (current.encodedPolyline() == null) == (withPassenger.encodedPolyline() == null);
        Integer extraMinutes = comparable && current.durationMinutes() != null
                && withPassenger.durationMinutes() != null
                ? Math.max(0, withPassenger.durationMinutes() - current.durationMinutes())
                : null;
        double extraKm = comparable ? Math.max(0, withPassenger.distanceKm() - current.distanceKm()) : 0;

        return new RidePreviewResponseDTO(
                current.encodedPolyline(),
                withPassenger.encodedPolyline(),
                current.distanceKm(),
                withPassenger.distanceKm(),
                extraKm,
                extraMinutes,
                withPassenger.durationMinutes(),
                stopsWithPassenger.stream().map(rideMapper::toStopResponse).toList()
        );
    }

    private RouteGeometry geometryOf(Ride ride, List<RideStop> stops) {
        return routeGeometry.compute(
                ride.getOrigin(),
                stops.stream().map(RideStop::place).toList(),
                ride.getDestination()
        );
    }
}
