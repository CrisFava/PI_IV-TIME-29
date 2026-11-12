// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.presentation.controller;

import com.converge.api.modules.ride.application.ConfirmPaymentUseCase;
import com.converge.api.modules.ride.application.GetPaymentUseCase;
import com.converge.api.modules.ride.application.RequestPaymentUseCase;
import com.converge.api.modules.ride.application.dto.PassengerRequestCommand;
import com.converge.api.modules.ride.application.dto.PaymentResponseDTO;
import com.converge.api.modules.ride.application.dto.RequestPaymentCommand;
import com.converge.api.modules.ride.application.dto.RequestPaymentRequestDTO;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pagamento de um passageiro em uma viagem (RF39, RF41): cobrança via Pix Copia e Cola e confirmação manual.
 */
@RestController
@RequestMapping("/api/v1/rides/{rideId}/passengers/{passengerId}/payment")
@RequiredArgsConstructor
public class RidePaymentController {

    private final RequestPaymentUseCase requestPayment;
    private final ConfirmPaymentUseCase confirmPayment;
    private final GetPaymentUseCase getPayment;

    @GetMapping
    public ResponseEntity<ApplicationResponse<PaymentResponseDTO>> getPayment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @PathVariable String passengerId
    ) {
        PaymentResponseDTO payment = getPayment.execute(
                new PassengerRequestCommand(rideId, passengerId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(payment));
    }

    // "Solicitar pagamento" (só o motorista)
    @PostMapping("/request")
    public ResponseEntity<ApplicationResponse<PaymentResponseDTO>> requestPayment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @PathVariable String passengerId,
            @Valid @RequestBody RequestPaymentRequestDTO request
    ) {
        PaymentResponseDTO payment = requestPayment.execute(
                new RequestPaymentCommand(rideId, passengerId, uid(principal), request));

        return ResponseEntity.ok(ApplicationResponse.success(payment));
    }

    // "Passageiro já pagou" (só o motorista)
    @PostMapping("/confirm")
    public ResponseEntity<ApplicationResponse<PaymentResponseDTO>> confirmPayment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @PathVariable String passengerId
    ) {
        PaymentResponseDTO payment = confirmPayment.execute(
                new PassengerRequestCommand(rideId, passengerId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(payment));
    }

    private String uid(AuthenticatedUser principal) {
        if (principal == null || principal.uid() == null) {
            throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS, "Não autenticado");
        }

        return principal.uid();
    }
}
