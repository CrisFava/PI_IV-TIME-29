package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.gateway.MapsGateway;
import com.converge.api.modules.route.application.gateway.PlaceDetails;
import com.converge.api.modules.route.application.gateway.PlaceSuggestion;

import java.util.List;
import java.util.Optional;

/**
 * Implementação falsa do MapsGateway para testes: respostas fixas, sem internet e sem custo.
 * Conta quantas vezes o autocomplete foi chamado, para os testes conferirem se o Google seria acionado.
 */
class FakeMapsGateway implements MapsGateway {

    static final PlaceSuggestion PUC_SUGGESTION = new PlaceSuggestion(
            "place-puc", "PUC Campinas Campus II", "Av. John Boyd Dunlop - Jardim Ipaussurama, Campinas - SP"
    );
    static final PlaceDetails PUC_DETAILS = new PlaceDetails(
            "Av. John Boyd Dunlop, s/n - Jardim Ipaussurama, Campinas - SP", "Jardim Ipaussurama", "Campinas",
            -22.9214685, -47.1194188
    );

    int autocompleteCalls = 0;

    @Override
    public List<PlaceSuggestion> autocomplete(String input, String sessionToken) {
        autocompleteCalls++;
        return List.of(PUC_SUGGESTION);
    }

    @Override
    public Optional<PlaceDetails> getPlaceDetails(String placeId, String sessionToken) {
        return "place-puc".equals(placeId) ? Optional.of(PUC_DETAILS) : Optional.empty();
    }
}
