package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.gateway.MapsGateway;
import com.converge.api.modules.route.application.gateway.PlaceDetails;
import com.converge.api.modules.route.application.gateway.PlaceSuggestion;
import com.converge.api.modules.route.application.gateway.RouteGeometry;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;

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

    static final RouteGeometry GOOGLE_ROUTE = new RouteGeometry(12.63, "encoded-polyline-do-google");

    int autocompleteCalls = 0;

    // Comportamento do computeRoute: devolve esta rota, a não ser que routesDown seja true
    Optional<RouteGeometry> nextRoute = Optional.of(GOOGLE_ROUTE);
    boolean routesDown = false;

    @Override
    public List<PlaceSuggestion> autocomplete(String input, String sessionToken) {
        autocompleteCalls++;
        return List.of(PUC_SUGGESTION);
    }

    @Override
    public Optional<PlaceDetails> getPlaceDetails(String placeId, String sessionToken) {
        return "place-puc".equals(placeId) ? Optional.of(PUC_DETAILS) : Optional.empty();
    }

    @Override
    public Optional<RouteGeometry> computeRoute(Coordinates origin, Coordinates destination) {
        if (routesDown) {
            throw new ApplicationException(ErrorCode.EXTERNAL_SERVICE_ERROR, "Google fora do ar (simulado).");
        }
        return nextRoute;
    }
}
