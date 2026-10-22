package com.converge.api.modules.route.infrastructure.google;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Formato dos JSONs da Places API (New). Só os campos que usamos: o Jackson ignora o resto.
 * Fica restrito a este pacote: nada fora do GoogleMapsGateway conhece o formato do Google.
 */
final class GooglePlacesJson {

    private GooglePlacesJson() {
    }

    // --- POST /places:autocomplete (requisição) ---

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record AutocompleteRequest(
            String input,
            String sessionToken,
            String languageCode,
            List<String> includedRegionCodes,
            LocationBias locationBias
    ) {
    }

    record LocationBias(Circle circle) {
    }

    record Circle(LatLng center, double radius) {
    }

    record LatLng(double latitude, double longitude) {
    }

    // --- POST /places:autocomplete (resposta) ---

    record AutocompleteResponse(List<Suggestion> suggestions) {
    }

    record Suggestion(PlacePrediction placePrediction) {
    }

    record PlacePrediction(String placeId, StructuredFormat structuredFormat) {
    }

    record StructuredFormat(FormattableText mainText, FormattableText secondaryText) {
    }

    record FormattableText(String text) {
    }

    // --- GET /places/{placeId} (resposta) ---

    record PlaceResponse(String formattedAddress, List<AddressComponent> addressComponents, LatLng location) {
    }

    record AddressComponent(String longText, List<String> types) {
    }
}
