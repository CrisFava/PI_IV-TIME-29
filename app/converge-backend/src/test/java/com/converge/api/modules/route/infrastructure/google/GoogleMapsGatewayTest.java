package com.converge.api.modules.route.infrastructure.google;

import com.converge.api.modules.route.application.gateway.PlaceDetails;
import com.converge.api.modules.route.application.gateway.PlaceSuggestion;
import com.converge.api.modules.route.application.gateway.RouteGeometry;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Testa a tradução dos JSONs do Google sem internet: o MockRestServiceServer faz o papel do Google.
 */
class GoogleMapsGatewayTest {

    private static final String AUTOCOMPLETE_URL = GoogleMapsGateway.PLACES_BASE_URL + "/places:autocomplete";
    private static final String DETAILS_URL = GoogleMapsGateway.PLACES_BASE_URL + "/places/place-puc";

    private MockRestServiceServer google;
    private GoogleMapsGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(GoogleMapsGateway.PLACES_BASE_URL);
        google = MockRestServiceServer.bindTo(builder).build();
        gateway = new GoogleMapsGateway(builder.build());
    }

    @Test
    @DisplayName("Deve traduzir as sugestões do Google, aceitando sugestão sem secondaryText")
    void shouldMapSuggestions() {
        google.expect(requestTo(AUTOCOMPLETE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        { "suggestions": [
                            { "placePrediction": {
                                "placeId": "place-puc",
                                "structuredFormat": {
                                  "mainText": { "text": "PUC Campinas Campus II" },
                                  "secondaryText": { "text": "Av. John Boyd Dunlop, Campinas - SP" } } } },
                            { "placePrediction": {
                                "placeId": "place-sem-linha",
                                "structuredFormat": { "mainText": { "text": "Campinas" } } } }
                        ] }
                        """, MediaType.APPLICATION_JSON));

        List<PlaceSuggestion> suggestions = gateway.autocomplete("puc campinas", "sessao-1");

        assertThat(suggestions).containsExactly(
                new PlaceSuggestion("place-puc", "PUC Campinas Campus II", "Av. John Boyd Dunlop, Campinas - SP"),
                new PlaceSuggestion("place-sem-linha", "Campinas", null)
        );
        google.verify();
    }

    @Test
    @DisplayName("Deve enviar ao Google o texto, o idioma, a região e o viés de Campinas")
    void shouldSendAutocompleteRequest() {
        google.expect(requestTo(AUTOCOMPLETE_URL))
                .andExpect(jsonPath("$.input").value("puc campinas"))
                .andExpect(jsonPath("$.sessionToken").value("sessao-1"))
                .andExpect(jsonPath("$.languageCode").value("pt-BR"))
                .andExpect(jsonPath("$.includedRegionCodes[0]").value("br"))
                .andExpect(jsonPath("$.locationBias.circle.center.latitude").value(-22.9056))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(gateway.autocomplete("puc campinas", "sessao-1")).isEmpty();
        google.verify();
    }

    @Test
    @DisplayName("Deve traduzir os detalhes do lugar, achando bairro e cidade nos addressComponents")
    void shouldMapPlaceDetails() {
        google.expect(requestTo(startsWith(DETAILS_URL)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Goog-FieldMask", "formattedAddress,addressComponents,location"))
                .andExpect(queryParam("sessionToken", "sessao-1"))
                .andRespond(withSuccess("""
                        { "formattedAddress": "Av. John Boyd Dunlop, s/n - Jardim Ipaussurama, Campinas - SP",
                          "addressComponents": [
                            { "longText": "Avenida John Boyd Dunlop", "types": ["route"] },
                            { "longText": "Jardim Ipaussurama", "types": ["sublocality_level_1", "sublocality", "political"] },
                            { "longText": "Campinas", "types": ["administrative_area_level_2", "political"] }
                          ],
                          "location": { "latitude": -22.9214685, "longitude": -47.1194188 } }
                        """, MediaType.APPLICATION_JSON));

        Optional<PlaceDetails> details = gateway.getPlaceDetails("place-puc", "sessao-1");

        assertThat(details).contains(new PlaceDetails(
                "Av. John Boyd Dunlop, s/n - Jardim Ipaussurama, Campinas - SP",
                "Jardim Ipaussurama",
                "Campinas",
                -22.9214685,
                -47.1194188
        ));
        google.verify();
    }

    @Test
    @DisplayName("Deve usar o locality como cidade quando não houver administrative_area_level_2")
    void shouldFallbackToLocalityForCity() {
        google.expect(requestTo(startsWith(DETAILS_URL)))
                .andRespond(withSuccess("""
                        { "formattedAddress": "Centro, Valinhos - SP",
                          "addressComponents": [ { "longText": "Valinhos", "types": ["locality", "political"] } ],
                          "location": { "latitude": -22.97, "longitude": -46.99 } }
                        """, MediaType.APPLICATION_JSON));

        PlaceDetails details = gateway.getPlaceDetails("place-puc", null).orElseThrow();

        assertThat(details.city()).isEqualTo("Valinhos");
        assertThat(details.neighborhood()).isNull();
    }

    @Test
    @DisplayName("Deve devolver Optional vazio quando o Google não encontrar o lugar")
    void shouldReturnEmptyForUnknownPlace() {
        google.expect(requestTo(startsWith(DETAILS_URL))).andRespond(withResourceNotFound());

        assertThat(gateway.getPlaceDetails("place-puc", "sessao-1")).isEmpty();
    }

    @Test
    @DisplayName("Deve lançar EXTERNAL_SERVICE_ERROR quando o Google falhar")
    void shouldTranslateGoogleFailure() {
        google.expect(requestTo(AUTOCOMPLETE_URL)).andRespond(withServerError());

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> gateway.autocomplete("puc campinas", "sessao-1")
        );

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    @Test
    @DisplayName("Deve traduzir a rota do Google: metros para km e a polyline")
    void shouldMapComputedRoute() {
        google.expect(requestTo(GoogleMapsGateway.ROUTES_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-FieldMask", "routes.distanceMeters,routes.polyline.encodedPolyline"))
                .andExpect(jsonPath("$.origin.location.latLng.latitude").value(-22.9056))
                .andExpect(jsonPath("$.destination.location.latLng.longitude").value(-47.0524))
                .andExpect(jsonPath("$.travelMode").value("DRIVE"))
                .andRespond(withSuccess("""
                        { "routes": [ { "distanceMeters": 12630, "polyline": { "encodedPolyline": "abc123" } } ] }
                        """, MediaType.APPLICATION_JSON));

        Optional<RouteGeometry> route = gateway.computeRoute(
                new Coordinates(-22.9056, -47.0608), new Coordinates(-22.8337, -47.0524)
        );

        assertThat(route).contains(new RouteGeometry(12.63, "abc123"));
        google.verify();
    }

    @Test
    @DisplayName("Deve devolver Optional vazio quando o Google não encontrar rota")
    void shouldReturnEmptyWhenNoRoute() {
        google.expect(requestTo(GoogleMapsGateway.ROUTES_URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(gateway.computeRoute(new Coordinates(0, 0), new Coordinates(1, 1))).isEmpty();
    }
}
