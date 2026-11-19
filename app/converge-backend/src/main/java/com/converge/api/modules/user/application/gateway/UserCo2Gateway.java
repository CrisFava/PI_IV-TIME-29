// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application.gateway;

// "Porta" para o módulo de indicadores. O user não lê a coleção de indicadores direto:
// pergunta por aqui. A implementação real é a IndicatorModuleUserCo2Gateway;
// nos testes usamos uma versão falsa.
public interface UserCo2Gateway {

    // CO₂ evitado acumulado do usuário, em kg (soma de todos os meses).
    double co2AvoidedKgOf(String userId);
}
