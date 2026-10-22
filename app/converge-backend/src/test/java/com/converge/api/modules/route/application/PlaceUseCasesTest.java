package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.dto.PlaceDetailsCommand;
import com.converge.api.modules.route.application.dto.SearchPlacesCommand;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.converge.api.modules.route.application.FakeMapsGateway.PUC_DETAILS;
import static com.converge.api.modules.route.application.FakeMapsGateway.PUC_SUGGESTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaceUseCasesTest {

    private FakeMapsGateway mapsGateway;
    private SearchPlacesUseCase searchPlaces;
    private GetPlaceDetailsUseCase getPlaceDetails;

    @BeforeEach
    void setUp() {
        mapsGateway = new FakeMapsGateway();
        searchPlaces = new SearchPlacesUseCase(mapsGateway);
        getPlaceDetails = new GetPlaceDetailsUseCase(mapsGateway);
    }

    @Test
    @DisplayName("Deve devolver as sugestões do serviço de mapas")
    void shouldReturnSuggestions() {
        assertThat(searchPlaces.execute(new SearchPlacesCommand("puc campinas", "sessao-1")))
                .containsExactly(PUC_SUGGESTION);
        assertThat(mapsGateway.autocompleteCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("Não deve chamar o serviço de mapas para textos com menos de 3 caracteres")
    void shouldNotCallMapsForShortInput() {
        assertThat(searchPlaces.execute(new SearchPlacesCommand("pu", "sessao-1"))).isEmpty();
        assertThat(searchPlaces.execute(new SearchPlacesCommand("  p  ", "sessao-1"))).isEmpty();
        assertThat(searchPlaces.execute(new SearchPlacesCommand(null, "sessao-1"))).isEmpty();
        assertThat(mapsGateway.autocompleteCalls).isZero();
    }

    @Test
    @DisplayName("Deve devolver os detalhes do lugar")
    void shouldReturnPlaceDetails() {
        assertThat(getPlaceDetails.execute(new PlaceDetailsCommand("place-puc", "sessao-1")))
                .isEqualTo(PUC_DETAILS);
    }

    @Test
    @DisplayName("Deve lançar PLACE_NOT_FOUND quando o lugar não existir")
    void shouldRejectUnknownPlace() {
        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> getPlaceDetails.execute(new PlaceDetailsCommand("place-inexistente", "sessao-1"))
        );

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.PLACE_NOT_FOUND);
    }
}
