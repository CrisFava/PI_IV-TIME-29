// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PaymentResponseDTO;
import com.converge.api.modules.ride.application.gateway.PixCharge;
import com.converge.api.modules.ride.application.gateway.PixPayloadGateway;
import com.converge.api.modules.ride.domain.enums.ParticipantPaymentStatus;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.user.application.GetUserPixInfoUseCase;
import com.converge.api.modules.user.application.dto.UserPixInfoDTO;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Monta a visão do pagamento de um passageiro, incluindo o Pix Copia e Cola enquanto a cobrança está em aberto.
 * Nada do código PIX é guardado: ele é derivado da chave do motorista e do valor solicitado.
 */
@Service
@RequiredArgsConstructor
public class RidePaymentViewService {

    private final GetUserPixInfoUseCase getUserPixInfo;
    private final PixPayloadGateway pixPayloadGateway;

    public void assertDriverCanCharge(String driverId) {
        if (!getUserPixInfo.execute(driverId).hasPixKey()) {
            throw new ApplicationException(
                    ErrorCode.NOT_ALLOWED,
                    "Cadastre uma chave PIX no seu perfil para solicitar pagamentos.",
                    Set.of("pixKey")
            );
        }
    }

    public PaymentResponseDTO view(Ride ride, String passengerId) {
        Participant passenger = findPassenger(ride, passengerId);

        UserPixInfoDTO driver = getUserPixInfo.execute(ride.getDriverId());

        return new PaymentResponseDTO(
                ride.getId() != null ? ride.getId().toHexString() : null,
                passenger.getUserId(),
                ride.getDriverId(),
                driver.name(),
                passenger.getRequestedAmount(),
                passenger.getPaymentStatus(),
                passenger.isPaymentConfirmedByDriver(),
                pixCopyPaste(ride, passenger, driver),
                passenger.getPaymentRequestedAt(),
                passenger.getPaidAt()
        );
    }

    // Quem saiu e pediu vaga de novo tem mais de uma participação: vale a que está com a vaga.
    private Participant findPassenger(Ride ride, String passengerId) {
        List<Participant> participations = ride.getParticipants().stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getUserId().equals(passengerId))
                .toList();

        return participations.stream()
                .filter(participant -> participant.getStatus() == ParticipantStatus.CONFIRMED
                        || participant.getStatus() == ParticipantStatus.COMPLETED)
                .findFirst()
                .or(() -> participations.stream().reduce((first, last) -> last))
                .orElseThrow(() -> new ApplicationException(ErrorCode.RIDE_NOT_FOUND, "Viagem não encontrada."));
    }

    private String pixCopyPaste(Ride ride, Participant passenger, UserPixInfoDTO driver) {
        boolean openCharge = passenger.getRequestedAmount() != null
                && passenger.getPaymentStatus() == ParticipantPaymentStatus.PENDING;

        if (!openCharge || !driver.hasPixKey()) {
            return null;
        }

        return pixPayloadGateway.generate(new PixCharge(
                driver.pixKey(),
                passenger.getRequestedAmount(),
                driver.name(),
                ride.getOrigin().city()
        ));
    }
}
