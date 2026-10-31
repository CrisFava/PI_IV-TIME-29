package com.converge.api.modules.ride.presentation.controller;

import com.converge.api.modules.ride.application.AcceptPassengerUseCase;
import com.converge.api.modules.ride.application.CancelRideUseCase;
import com.converge.api.modules.ride.application.CompleteRideUseCase;
import com.converge.api.modules.ride.application.GetRideUseCase;
import com.converge.api.modules.ride.application.LeaveRideUseCase;
import com.converge.api.modules.ride.application.ListMyRidesUseCase;
import com.converge.api.modules.ride.application.MarkNoShowUseCase;
import com.converge.api.modules.ride.application.RejectPassengerUseCase;
import com.converge.api.modules.ride.application.RequestRideSeatUseCase;
import com.converge.api.modules.ride.application.StartRideUseCase;
import com.converge.api.modules.ride.application.dto.CancelRideCommand;
import com.converge.api.modules.ride.application.dto.CancelRideRequestDTO;
import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.dto.RejectPassengerCommand;
import com.converge.api.modules.ride.application.dto.RejectPassengerRequestDTO;
import com.converge.api.modules.ride.application.dto.RequestRideSeatCommand;
import com.converge.api.modules.ride.application.dto.RequestRideSeatRequestDTO;
import com.converge.api.modules.ride.application.dto.RideRefCommand;
import com.converge.api.modules.ride.application.dto.RideResponseDTO;
import com.converge.api.modules.ride.application.mapper.RideMapper;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
public class RideController {

    private final RequestRideSeatUseCase requestRideSeat;
    private final StartRideUseCase startRide;
    private final CompleteRideUseCase completeRide;
    private final MarkNoShowUseCase markNoShow;
    private final LeaveRideUseCase leaveRide;
    private final AcceptPassengerUseCase acceptPassenger;
    private final RejectPassengerUseCase rejectPassenger;
    private final ListMyRidesUseCase listMyRides;
    private final GetRideUseCase getRide;
    private final CancelRideUseCase cancelRide;
    private final RideMapper mapper;

    @PostMapping("/request")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> requestSeat(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody RequestRideSeatRequestDTO request
    ) {
        Ride ride = requestRideSeat.execute(new RequestRideSeatCommand(uid(principal), request));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApplicationResponse.success(mapper.toResponse(ride)));
    }

    @GetMapping
    public ResponseEntity<ApplicationResponse<List<RideResponseDTO>>> listMyRides(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        List<RideResponseDTO> rides = listMyRides.execute(uid(principal))
                .stream()
                .map(mapper::toResponse)
                .toList();

        return ResponseEntity.ok(ApplicationResponse.success(rides));
    }

    @GetMapping("/{rideId}")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> getRide(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId
    ) {
        Ride ride = getRide.execute(new RideRefCommand(rideId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(ride)));
    }

    @PostMapping("/{rideId}/cancel")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> cancelRide(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @RequestBody(required = false) CancelRideRequestDTO request
    ) {
        CancelRideRequestDTO body = request != null ? request : new CancelRideRequestDTO(null);
        Ride ride = cancelRide.execute(new CancelRideCommand(rideId, uid(principal), body));

        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(ride)));
    }

    @PostMapping("/{rideId}/start")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> startRide(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId
    ) {
        Ride ride = startRide.execute(new RideRefCommand(rideId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(ride)));
    }

    @PostMapping("/{rideId}/complete")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> completeRide(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId
    ) {
        Ride ride = completeRide.execute(new RideRefCommand(rideId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(ride)));
    }

    @PostMapping("/{rideId}/passengers/{passengerId}/no-show")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> markNoShow(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @PathVariable String passengerId
    ) {
        Ride ride = markNoShow.execute(new PassengerRequestCommand(rideId, passengerId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(ride)));
    }

    @PostMapping("/{rideId}/leave")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> leaveRide(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @RequestBody(required = false) CancelRideRequestDTO request
    ) {
        CancelRideRequestDTO body = request != null ? request : new CancelRideRequestDTO(null);
        Ride ride = leaveRide.execute(new CancelRideCommand(rideId, uid(principal), body));

        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(ride)));
    }

    @PostMapping("/{rideId}/passengers/{passengerId}/accept")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> acceptPassenger(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @PathVariable String passengerId
    ) {
        Ride ride = acceptPassenger.execute(new PassengerRequestCommand(rideId, passengerId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(ride)));
    }

    @PostMapping("/{rideId}/passengers/{passengerId}/reject")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> rejectPassenger(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @PathVariable String passengerId,
            @RequestBody(required = false) RejectPassengerRequestDTO request
    ) {
        RejectPassengerRequestDTO body = request != null ? request : new RejectPassengerRequestDTO(null);
        Ride ride = rejectPassenger.execute(new RejectPassengerCommand(rideId, passengerId, uid(principal), body));

        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(ride)));
    }

    private String uid(AuthenticatedUser principal) {
        if (principal == null || principal.uid() == null) {
            throw new ApplicationException(
                ErrorCode.INVALID_CREDENTIALS,
                "Não autenticado"
            );
        }

        return principal.uid();
    }

}
