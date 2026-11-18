// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application.gateway;

import java.util.Collection;
import java.util.Map;

// Porta para o módulo de usuários. O security não lê a coleção de usuários direto.
public interface UserLookupGateway {

    // O usuário existe?
    boolean exists(String userId);

    // Nomes por id (ids desconhecidos ficam de fora do mapa)
    Map<String, String> namesOf(Collection<String> userIds);
}
