// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.infrastructure;

import com.converge.api.modules.review.application.gateway.RideParticipationGateway;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Versão TEMPORÁRIA do gateway de viagens: sempre responde "viagem não encontrada".
// Serve só para a aplicação subir enquanto o módulo ride não existe.
// Quando o ride ficar pronto, criar a implementação real (que pergunta à viagem de verdade)
// e remover esta classe.
@Component
public class StubRideParticipationGateway implements RideParticipationGateway {

    @Override
    public Optional<CompletedRide> findCompletedRide(String rideId) {
        return Optional.empty();
    }
}
