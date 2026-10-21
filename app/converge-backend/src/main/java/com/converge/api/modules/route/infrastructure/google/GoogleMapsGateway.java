package com.converge.api.modules.route.infrastructure.google;

import com.converge.api.modules.route.application.gateway.MapsGateway;
import com.converge.api.modules.route.application.gateway.PlaceDetails;
import com.converge.api.modules.route.application.gateway.PlaceSuggestion;
import com.converge.api.modules.route.infrastructure.google.GooglePlacesJson.AddressComponent;
import com.converge.api.modules.route.infrastructure.google.GooglePlacesJson.AutocompleteRequest;
import com.converge.api.modules.route.infrastructure.google.GooglePlacesJson.AutocompleteResponse;
import com.converge.api.modules.route.infrastructure.google.GooglePlacesJson.Circle;
import com.converge.api.modules.route.infrastructure.google.GooglePlacesJson.FormattableText;
import com.converge.api.modules.route.infrastructure.google.GooglePlacesJson.LatLng;
import com.converge.api.modules.route.infrastructure.google.GooglePlacesJson.LocationBias;
import com.converge.api.modules.route.infrastructure.google.GooglePlacesJson.PlacePrediction;
import com.converge.api.modules.route.infrastructure.google.GooglePlacesJson.PlaceResponse;
import com.converge.api.modules.route.infrastructure.google.GooglePlacesJson.Suggestion;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Component
public class GoogleMapsGateway implements MapsGateway {

    static final String PLACES_BASE_URL = "https://places.googleapis.com/v1";

    private static final String API_KEY_HEADER = "X-Goog-Api-Key";
    private static final String FIELD_MASK_HEADER = "X-Goog-FieldMask";
    private static final String DETAILS_FIELD_MASK = "formattedAddress,addressComponents,location";
    private static final String LANGUAGE = "pt-BR";
    private static final List<String> REGIONS = List.of("br");

    // Centro de Campinas, raio de 30 km: prioriza a região sem excluir outras cidades
    private static final LocationBias CAMPINAS_BIAS =
            new LocationBias(new Circle(new LatLng(-22.9056, -47.0608), 30_000));

    private final RestClient restClient;

    @Autowired
    public GoogleMapsGateway(
            @Value("${google.maps.api-key}") String apiKey,
            @Value("${google.maps.timeout}") Duration timeout
    ) {
        this(RestClient.builder()
                .baseUrl(PLACES_BASE_URL)
                .defaultHeader(API_KEY_HEADER, apiKey)
                .requestFactory(requestFactoryWith(timeout))
                .build());

        if (apiKey.isBlank()) {
            log.warn("GOOGLE_MAPS_API_KEY não configurada: as chamadas ao Google vão falhar.");
        }
    }

    // Usado nos testes, com um RestClient apontando para um servidor falso
    GoogleMapsGateway(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<PlaceSuggestion> autocomplete(String input, String sessionToken) {
        AutocompleteRequest request = new AutocompleteRequest(input, sessionToken, LANGUAGE, REGIONS, CAMPINAS_BIAS);

        AutocompleteResponse response = call(() -> restClient.post()
                .uri("/places:autocomplete")
                .body(request)
                .retrieve()
                .body(AutocompleteResponse.class));

        if (response == null || response.suggestions() == null) {
            return List.of();
        }

        return response.suggestions().stream()
                .map(Suggestion::placePrediction)
                .filter(Objects::nonNull)
                .map(this::toSuggestion)
                .toList();
    }

    @Override
    public Optional<PlaceDetails> getPlaceDetails(String placeId, String sessionToken) {
        PlaceResponse response = call(() -> {
            try {
                return restClient.get()
                        .uri(uri -> uri.path("/places/{placeId}")
                                .queryParam("languageCode", LANGUAGE)
                                .queryParamIfPresent("sessionToken", Optional.ofNullable(sessionToken))
                                .build(placeId))
                        .header(FIELD_MASK_HEADER, DETAILS_FIELD_MASK)
                        .retrieve()
                        .body(PlaceResponse.class);
            } catch (HttpClientErrorException e) {
                // O Google responde 400/404 para placeId inexistente ou malformado
                if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                    return null;
                }
                throw e;
            }
        });

        return Optional.ofNullable(response).map(this::toDetails);
    }

    private PlaceSuggestion toSuggestion(PlacePrediction prediction) {
        FormattableText secondary = prediction.structuredFormat().secondaryText();
        String secondaryText = secondary != null ? secondary.text() : null;

        return new PlaceSuggestion(
            prediction.placeId(), 
            prediction.structuredFormat().mainText().text(), 
            secondaryText
        );
    }

    private PlaceDetails toDetails(PlaceResponse place) {
        List<AddressComponent> components = place.addressComponents() != null ? place.addressComponents() : List.of();

        return new PlaceDetails(
                place.formattedAddress(),
                findComponent(components, "sublocality").orElse(null),
                findComponent(components, "administrative_area_level_2")
                        .or(() -> findComponent(components, "locality"))
                        .orElse(null),
                place.location().latitude(),
                place.location().longitude()
        );
    }

    private Optional<String> findComponent(List<AddressComponent> components, String type) {
        return components.stream()
                .filter(component -> component.types().contains(type))
                .map(AddressComponent::longText)
                .findFirst();
    }


    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException e) {
            log.warn("Falha ao chamar o Google Maps: {}", e.getMessage());
            throw new ApplicationException(
                    ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "O serviço de mapas está indisponível no momento. Tente novamente."
            );
        }
    }

    private static JdkClientHttpRequestFactory requestFactoryWith(Duration timeout) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build()
        );
        requestFactory.setReadTimeout(timeout);
        return requestFactory;
    }
}
