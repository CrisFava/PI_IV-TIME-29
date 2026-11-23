// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.presentation.controller;

import com.converge.api.modules.ride.application.GetPaymentUseCase;
import com.converge.api.modules.ride.application.MarkPaymentAsPaidUseCase;
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
 * Pagamento de um passageiro em uma viagem (RF39, RF41): cobrança via Pix Copia e Cola e o "Já paguei" do passageiro.
 *
 * Todas as rotas exigem o token do Firebase (Authorization: Bearer) e respondem no envelope ApplicationResponse.
 * A cobrança em si é emitida pelos casos de uso da viagem (chegada do passageiro e encerramento); aqui ficam
 * a consulta, a cobrança manual e o "Já paguei".
 */
@RestController
@RequestMapping("/api/v1/rides/{rideId}/passengers/{passengerId}/payment")
@RequiredArgsConstructor
public class RidePaymentController {

    private final RequestPaymentUseCase requestPayment;
    private final GetPaymentUseCase getPayment;
    private final MarkPaymentAsPaidUseCase markPaymentAsPaid;

    /**
     * GET .../payment: consulta a cobrança do passageiro. Só o motorista e o próprio passageiro enxergam.
     *
     * @param principal   usuário autenticado pelo filtro do Firebase
     * @param rideId      id da viagem
     * @param passengerId id (Mongo) do passageiro
     * @return 200 com a situação do pagamento e o Pix Copia e Cola, se a cobrança estiver aberta
     */
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

    /**
     * POST .../payment/request: "Solicitar pagamento" com um valor informado (só o motorista).
     * Mantido na API, mas o app não usa: a cobrança sai automática com o valor do rateio.
     *
     * @param request corpo com o valor, validado por @Valid
     * @return 200 com a cobrança emitida e o PIX
     */
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

    /**
     * POST .../payment/mark-paid: "Já paguei" (só o próprio passageiro). Libera o passageiro para outras
     * caronas e avisa o motorista. Repetir a chamada não muda nada.
     *
     * @return 200 com a cobrança já paga
     */
    @PostMapping("/mark-paid")
    public ResponseEntity<ApplicationResponse<PaymentResponseDTO>> markAsPaid(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String rideId,
            @PathVariable String passengerId
    ) {
        PaymentResponseDTO payment = markPaymentAsPaid.execute(
                new PassengerRequestCommand(rideId, passengerId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(payment));
    }

    /**
     * Extrai o uid do Firebase do usuário autenticado.
     *
     * @throws ApplicationException com INVALID_CREDENTIALS se a requisição chegou sem usuário autenticado
     */
    private String uid(AuthenticatedUser principal) {
        if (principal == null || principal.uid() == null) {
            throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS, "Não autenticado");
        }

        return principal.uid();
    }
}
