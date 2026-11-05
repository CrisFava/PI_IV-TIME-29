// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.review.application.gateway;

import com.converge.api.modules.review.domain.enums.ReviewedRole;

import java.util.List;
import java.util.Optional;

// "Porta" para o módulo de viagens. O Review não lê a coleção de viagens direto:
// pergunta por aqui. A implementação real vem quando o módulo ride existir;
// nos testes usamos uma versão falsa.
public interface RideParticipationGateway {

    // Devolve os dados da viagem concluída, ou vazio se não existe / não terminou.
    Optional<CompletedRide> findCompletedRide(String rideId);

    // Visão enxuta da viagem: só o que o Review precisa para validar a avaliação.
    record CompletedRide(String rideId, List<Participant> participants) {
    }

    // Um participante da viagem, com o papel dele.
    record Participant(String userId, ReviewedRole role) {
    }
}
