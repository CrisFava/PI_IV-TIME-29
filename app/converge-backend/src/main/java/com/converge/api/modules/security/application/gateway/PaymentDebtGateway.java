// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application.gateway;

// Porta para o módulo de viagens: a denúncia de falta de pagamento vira uma dívida do passageiro.
public interface PaymentDebtGateway {

    // Devolve true quando a dívida foi registrada; false quando não há cobrança paga ou ela já foi denunciada.
    boolean registerNonPayment(String rideId, String passengerId);
}
