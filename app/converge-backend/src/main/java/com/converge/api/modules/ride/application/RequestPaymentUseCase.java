// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PaymentResponseDTO;
import com.converge.api.modules.ride.application.dto.RequestPaymentCommand;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * "Solicitar pagamento": o motorista cobra de um passageiro confirmado o valor combinado no chat
 * e recebe o Pix Copia e Cola da cobrança.
 *
 * O endpoint continua na API (POST .../payment/request), mas o app não o usa mais: hoje a cobrança é
 * emitida sozinha com o valor do rateio, na chegada do passageiro ou no encerramento da viagem (RF39).
 */
@Service
@RequiredArgsConstructor
public class RequestPaymentUseCase implements UseCase<RequestPaymentCommand, PaymentResponseDTO> {

    private final RideAccess rideAccess;
    private final RideRepository rideRepository;
    private final RidePaymentViewService paymentView;

    /**
     * Grava a cobrança com o valor informado e devolve a visão dela com o PIX.
     *
     * @param command viagem, passageiro, valor e firebaseUid do motorista
     * @return a cobrança emitida, com o Pix Copia e Cola
     * @throws ApplicationException com NOT_ALLOWED se o motorista não tem chave PIX, além dos erros de
     *                              Ride.requestPayment (passageiro sem vaga, valor acima do teto etc.)
     */
    @Override
    public PaymentResponseDTO execute(RequestPaymentCommand command) {
        Ride ride = rideAccess.getOwnedRide(command.rideId(), command.firebaseUid());

        paymentView.assertDriverCanCharge(ride.getDriverId());
        ride.requestPayment(command.passengerId(), command.request().amount());

        // Gera a resposta (e o PIX) antes de gravar: se o código não puder ser gerado, a cobrança não fica salva.
        PaymentResponseDTO response = paymentView.view(ride, command.passengerId());
        rideRepository.save(ride);

        return response;
    }
}
