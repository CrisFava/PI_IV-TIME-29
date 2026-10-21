package com.converge.api.modules.route.application.gateway;

import java.util.List;
import java.util.Optional;

/**
 * Contrato com o serviço de mapas. A aplicação depende só desta interface:
 * quem implementa (Google, ou um fake nos testes) fica em infrastructure/.
 */
public interface MapsGateway {
    List<PlaceSuggestion> autocomplete(String input, String sessionToken);

    Optional<PlaceDetails> getPlaceDetails(String placeId, String sessionToken);
}
