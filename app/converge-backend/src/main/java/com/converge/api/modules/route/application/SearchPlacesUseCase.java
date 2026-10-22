package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.dto.SearchPlacesCommand;
import com.converge.api.modules.route.application.gateway.MapsGateway;
import com.converge.api.modules.route.application.gateway.PlaceSuggestion;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchPlacesUseCase implements UseCase<SearchPlacesCommand, List<PlaceSuggestion>> {

    // Cada chamada ao Google custa: textos muito curtos não geram sugestões úteis
    private static final int MIN_INPUT_LENGTH = 3;

    private final MapsGateway mapsGateway;

    @Override
    public List<PlaceSuggestion> execute(SearchPlacesCommand command) {
        if (command.input() == null || command.input().strip().length() < MIN_INPUT_LENGTH) {
            return List.of();
        }

        return mapsGateway.autocomplete(command.input(), command.sessionToken());   
    }
}
