// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.presentation.controller;

import com.converge.api.modules.matching.application.ListRouteMatchingsUseCase;
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
@RequestMapping("/api/v1/matchings")
@RequiredArgsConstructor
public class MatchingController {

    private final ListRouteMatchingsUseCase listRouteMatchings;
    private final UserUtils userUtils;

    // Combinações compatíveis com um trajeto do usuário, da mais para a menos compatível.
    // Trajeto de procura: motoristas; trajeto de oferta: passageiros ("N passageiros x 1 motorista").
    @GetMapping
    public ResponseEntity<ApplicationResponse<List<MatchResponseDTO>>> listMatchings(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam String routeId
    ) {
        String userId = userUtils.getCurrentUser(principal).getId().toHexString();
        List<MatchResponseDTO> matches = listRouteMatchings.execute(new FindMatchesCommand(routeId, userId));

        return ResponseEntity.ok(ApplicationResponse.success(matches));
    }
}
