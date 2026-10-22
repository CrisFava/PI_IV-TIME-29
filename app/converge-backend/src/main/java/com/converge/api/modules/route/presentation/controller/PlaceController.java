package com.converge.api.modules.route.presentation.controller;

import com.converge.api.modules.route.application.GetPlaceDetailsUseCase;
import com.converge.api.modules.route.application.SearchPlacesUseCase;
import com.converge.api.modules.route.application.dto.PlaceDetailsCommand;
import com.converge.api.modules.route.application.dto.SearchPlacesCommand;
import com.converge.api.modules.route.application.gateway.PlaceDetails;
import com.converge.api.modules.route.application.gateway.PlaceSuggestion;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    private final SearchPlacesUseCase searchPlaces;
    private final GetPlaceDetailsUseCase getPlaceDetails;

    // GET /api/v1/places/autocomplete?input=puc campinas&sessionToken=abc
    @GetMapping("/autocomplete")
    public ResponseEntity<ApplicationResponse<List<PlaceSuggestion>>> autocomplete(
            @RequestParam String input,
            @RequestParam(required = false) String sessionToken) {
        List<PlaceSuggestion> suggestions = searchPlaces.execute(new SearchPlacesCommand(input, sessionToken));

        return ResponseEntity.ok(ApplicationResponse.success(suggestions));
    }

    @GetMapping("/{placeId}")
    public ResponseEntity<ApplicationResponse<PlaceDetails>> getPlaceDetails(
            @PathVariable String placeId,
            @RequestParam(required = false) String sessionToken) {
        PlaceDetails details = getPlaceDetails.execute(new PlaceDetailsCommand(placeId, sessionToken));

        return ResponseEntity.ok(ApplicationResponse.success(details));
    }

}
