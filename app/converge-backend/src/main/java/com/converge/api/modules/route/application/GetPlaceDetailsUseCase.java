// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.dto.PlaceDetailsCommand;
import com.converge.api.modules.route.application.gateway.MapsGateway;
import com.converge.api.modules.route.application.gateway.PlaceDetails;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Caso de uso que busca os detalhes de um lugar (endereço, coordenadas etc.)
 * a partir do identificador dele (placeId) em um serviço de mapas.
 *
 * Não acessa o provedor diretamente: delega ao MapsGateway, que esconde qual
 * serviço de mapas está por trás. Se o lugar não for encontrado, lança
 * PLACE_NOT_FOUND.
 */
@Service
@RequiredArgsConstructor
public class GetPlaceDetailsUseCase implements UseCase<PlaceDetailsCommand, PlaceDetails> {

    private final MapsGateway mapsGateway;

    @Override
    public PlaceDetails execute(PlaceDetailsCommand command) {
        return mapsGateway.getPlaceDetails(command.placeId(), command.sessionToken())
        .orElseThrow(() -> new ApplicationException(ErrorCode.PLACE_NOT_FOUND, "Lugar não encontrado."));
    }
}
