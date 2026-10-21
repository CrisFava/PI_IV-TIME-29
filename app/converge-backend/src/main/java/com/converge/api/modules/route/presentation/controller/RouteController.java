package com.converge.api.modules.route.presentation.controller;

import com.converge.api.modules.route.application.CreateRouteUseCase;
import com.converge.api.modules.route.application.dto.CreateRouteCommand;
import com.converge.api.modules.route.application.dto.CreateRouteRequestDTO;
import com.converge.api.modules.route.application.dto.RouteResponseDTO;
import com.converge.api.modules.route.application.mapper.RouteMapper;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {

    private final CreateRouteUseCase createRoute;
    private final UserUtils userUtils;
    private final RouteMapper mapper;

    @PostMapping
    public ResponseEntity<ApplicationResponse<RouteResponseDTO>> createRoute(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateRouteRequestDTO request
    ) {
        User user = userUtils.getCurrentUser(principal);
        Route route = createRoute.execute(new CreateRouteCommand(user.getId().toHexString(), request));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApplicationResponse.success(mapper.toResponse(route)));
    }
}
