// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.presentation.controller;

import com.converge.api.modules.matching.application.FindMatchesForPassengerUseCase;
import com.converge.api.modules.matching.application.FindPassengersForDriverUseCase;
import com.converge.api.modules.matching.application.dto.FindMatchesCommand;
import com.converge.api.modules.matching.application.dto.MatchResponseDTO;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final FindMatchesForPassengerUseCase findMatchesForPassenger;
    private final FindPassengersForDriverUseCase findPassengersForDriver;
    private final UserUtils userUtils;

    // Motoristas compatíveis com um trajeto de procura do usuário, do mais para o menos compatível
    @GetMapping("/passenger")
    public ResponseEntity<ApplicationResponse<List<MatchResponseDTO>>> findDrivers(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam String routeId
    ) {
        List<MatchResponseDTO> matches = findMatchesForPassenger.execute(
                new FindMatchesCommand(routeId, currentUserId(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(matches));
    }

    // Passageiros compatíveis com um trajeto de oferta do usuário, do mais para o menos compatível
    @GetMapping("/driver")
    public ResponseEntity<ApplicationResponse<List<MatchResponseDTO>>> findPassengers(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam String routeId
    ) {
        List<MatchResponseDTO> matches = findPassengersForDriver.execute(
                new FindMatchesCommand(routeId, currentUserId(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(matches));
    }

    private String currentUserId(AuthenticatedUser principal) {
        return userUtils.getCurrentUser(principal).getId().toHexString();
    }
}
