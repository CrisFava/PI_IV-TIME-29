package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.dto.PlaceDetailsCommand;
import com.converge.api.modules.route.application.gateway.MapsGateway;
import com.converge.api.modules.route.application.gateway.PlaceDetails;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
