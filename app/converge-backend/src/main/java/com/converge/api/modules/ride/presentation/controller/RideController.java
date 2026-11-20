// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.ride.presentation.controller;

import java.time.LocalDate;
import org.springframework.web.bind.annotation.RequestParam;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.modules.ride.application.dto.RideHistoryQuery;
import com.converge.api.modules.ride.application.dto.RideHistoryItemDTO;
import com.converge.api.modules.ride.application.ListRideHistoryUseCase;
import com.converge.api.modules.ride.application.AcceptPassengerUseCase;
import com.converge.api.modules.ride.application.CancelRideUseCase;
import com.converge.api.modules.ride.application.CompleteRideUseCase;
import com.converge.api.modules.ride.application.GetRideUseCase;
import com.converge.api.modules.ride.application.LeaveRideUseCase;
import com.converge.api.modules.ride.application.ListMyRidesUseCase;
import com.converge.api.modules.ride.application.ArrivePassengerUseCase;
import com.converge.api.modules.ride.application.ListPendingPaymentsUseCase;
import com.converge.api.modules.ride.application.BoardPassengerUseCase;
import com.converge.api.modules.ride.application.MarkNoShowUseCase;
import com.converge.api.modules.ride.application.PreviewPassengerRideUseCase;
import com.converge.api.modules.ride.application.dto.RidePreviewResponseDTO;
import com.converge.api.modules.ride.application.dto.PaymentResponseDTO;
import com.converge.api.modules.ride.application.RejectPassengerUseCase;
import com.converge.api.modules.ride.application.RequestRideSeatUseCase;
import com.converge.api.modules.ride.application.RideViewService;
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
    private final ArrivePassengerUseCase arrivePassenger;
    private final BoardPassengerUseCase boardPassenger;
    private final PreviewPassengerRideUseCase previewPassengerRide;
    private final ListPendingPaymentsUseCase listPendingPayments;
    private final LeaveRideUseCase leaveRide;
    private final AcceptPassengerUseCase acceptPassenger;
    private final RejectPassengerUseCase rejectPassenger;
    private final ListMyRidesUseCase listMyRides;
    private final ListRideHistoryUseCase listRideHistory;
    private final GetRideUseCase getRide;
    private final CancelRideUseCase cancelRide;
    private final RideViewService rideView;

    @PostMapping("/request")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> requestSeat(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody RequestRideSeatRequestDTO request
    ) {
        Ride ride = requestRideSeat.execute(new RequestRideSeatCommand(uid(principal), request));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApplicationResponse.success(rideView.view(ride, uid(principal))));
    }

    @GetMapping
    public ResponseEntity<ApplicationResponse<List<RideResponseDTO>>> listMyRides(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        String uid = uid(principal);

        List<RideResponseDTO> rides = rideView.viewAll(listMyRides.execute(uid), uid);

        return ResponseEntity.ok(ApplicationResponse.success(rides));
    }

    // Histórico (RF59): viagens terminadas, com filtro de papel e de datas da saída (yyyy-MM-dd)
    @GetMapping("/history")
    public ResponseEntity<ApplicationResponse<List<RideHistoryItemDTO>>> listHistory(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) ParticipationRole role,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        List<RideHistoryItemDTO> history = listRideHistory.execute(
                new RideHistoryQuery(uid(principal), role, from, to));

        return ResponseEntity.ok(ApplicationResponse.success(history));
    }

    // Cobranças em aberto do usuário logado: o app abre o pagamento com elas
    @GetMapping("/payments/pending")
    public ResponseEntity<ApplicationResponse<List<PaymentResponseDTO>>> listPendingPayments(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(ApplicationResponse.success(listPendingPayments.execute(uid(principal))));
    }

    @GetMapping("/{rideId}")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> getRide(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId
    ) {
        Ride ride = getRide.execute(new RideRefCommand(rideId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(rideView.view(ride, uid(principal))));
    }

    @PostMapping("/{rideId}/cancel")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> cancelRide(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @RequestBody(required = false) CancelRideRequestDTO request
    ) {
        CancelRideRequestDTO body = request != null ? request : new CancelRideRequestDTO(null);
        Ride ride = cancelRide.execute(new CancelRideCommand(rideId, uid(principal), body));

        return ResponseEntity.ok(ApplicationResponse.success(rideView.view(ride, uid(principal))));
    }

    @PostMapping("/{rideId}/start")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> startRide(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId
    ) {
        Ride ride = startRide.execute(new RideRefCommand(rideId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(rideView.view(ride, uid(principal))));
    }

    @PostMapping("/{rideId}/complete")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> completeRide(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId
    ) {
        Ride ride = completeRide.execute(new RideRefCommand(rideId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(rideView.view(ride, uid(principal))));
    }

    // Como ficaria a viagem aceitando o pedido (só o motorista): os dois caminhos e o desvio
    @GetMapping("/{rideId}/passengers/{passengerId}/preview")
    public ResponseEntity<ApplicationResponse<RidePreviewResponseDTO>> previewPassenger(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @PathVariable String passengerId
    ) {
        RidePreviewResponseDTO preview = previewPassengerRide.execute(
                new PassengerRequestCommand(rideId, passengerId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(preview));
    }

    // "Entrou" (só o motorista): o passageiro embarcou
    @PostMapping("/{rideId}/passengers/{passengerId}/board")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> boardPassenger(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @PathVariable String passengerId
    ) {
        Ride ride = boardPassenger.execute(new PassengerRequestCommand(rideId, passengerId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(rideView.view(ride, uid(principal))));
    }

    // "Confirmar chegada" (só o motorista): o passageiro desceu e já recebe a cobrança
    @PostMapping("/{rideId}/passengers/{passengerId}/arrive")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> arrivePassenger(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @PathVariable String passengerId
    ) {
        Ride ride = arrivePassenger.execute(new PassengerRequestCommand(rideId, passengerId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(rideView.view(ride, uid(principal))));
    }

    @PostMapping("/{rideId}/passengers/{passengerId}/no-show")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> markNoShow(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @PathVariable String passengerId
    ) {
        Ride ride = markNoShow.execute(new PassengerRequestCommand(rideId, passengerId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(rideView.view(ride, uid(principal))));
    }

    @PostMapping("/{rideId}/leave")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> leaveRide(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @RequestBody(required = false) CancelRideRequestDTO request
    ) {
        CancelRideRequestDTO body = request != null ? request : new CancelRideRequestDTO(null);
        Ride ride = leaveRide.execute(new CancelRideCommand(rideId, uid(principal), body));

        return ResponseEntity.ok(ApplicationResponse.success(rideView.view(ride, uid(principal))));
    }

    @PostMapping("/{rideId}/passengers/{passengerId}/accept")
    public ResponseEntity<ApplicationResponse<RideResponseDTO>> acceptPassenger(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @PathVariable String passengerId
    ) {
        Ride ride = acceptPassenger.execute(new PassengerRequestCommand(rideId, passengerId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(rideView.view(ride, uid(principal))));
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

        return ResponseEntity.ok(ApplicationResponse.success(rideView.view(ride, uid(principal))));
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
