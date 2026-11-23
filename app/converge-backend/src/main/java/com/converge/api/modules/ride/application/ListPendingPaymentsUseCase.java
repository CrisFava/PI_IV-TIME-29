// Autor: Cristian Eduardo Fava - RA 25000636

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.PaymentResponseDTO;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Cobranças em aberto do usuário logado como passageiro, com o PIX de cada uma.
 * É o que o app consulta para abrir o pagamento quando a viagem acaba (ou ao reabrir o app).
 */
@Service
@RequiredArgsConstructor
public class ListPendingPaymentsUseCase implements UseCase<String, List<PaymentResponseDTO>> {

    private final RideAccess rideAccess;
    private final RideRepository rideRepository;
    private final RidePaymentViewService paymentView;

    /**
     * Percorre as viagens em que o usuário participa e devolve as que têm cobrança emitida e ainda
     * PENDING para ele (RF68).
     *
     * @param firebaseUid uid do Firebase do usuário logado
     * @return uma visão de pagamento por cobrança em aberto, com o Pix Copia e Cola; lista vazia se estiver em dia
     */
    @Override
    public List<PaymentResponseDTO> execute(String firebaseUid) {
        String userId = rideAccess.currentUserId(firebaseUid);

        return rideRepository.findByParticipantsUserId(userId).stream()
                .filter(ride -> ride.hasOpenChargeFor(userId))
                .map(ride -> paymentView.view(ride, userId))
                .toList();
    }
}
