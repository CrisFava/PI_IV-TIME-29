package com.converge.api.modules.route.presentation.controller;

import com.converge.api.modules.route.application.CreateRouteUseCase;
import com.converge.api.modules.route.application.DeleteRouteUseCase;
import com.converge.api.modules.route.application.GetRouteUseCase;
import com.converge.api.modules.route.application.ListMyRoutesUseCase;
import com.converge.api.modules.route.application.UpdateRouteUseCase;
import com.converge.api.modules.route.application.dto.CreateRouteCommand;
import com.converge.api.modules.route.application.dto.CreateRouteRequestDTO;
import com.converge.api.modules.route.application.dto.RouteRefCommand;
import com.converge.api.modules.route.application.dto.RouteResponseDTO;
import com.converge.api.modules.route.application.dto.UpdateRouteCommand;
import com.converge.api.modules.route.application.dto.UpdateRouteRequestDTO;
import com.converge.api.modules.route.application.mapper.RouteMapper;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {

    private final CreateRouteUseCase createRoute;
    private final ListMyRoutesUseCase listMyRoutes;
    private final GetRouteUseCase getRoute;
    private final UpdateRouteUseCase updateRoute;
    private final DeleteRouteUseCase deleteRoute;
    private final UserUtils userUtils;
    private final RouteMapper mapper;

    @PostMapping
    public ResponseEntity<ApplicationResponse<RouteResponseDTO>> createRoute(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateRouteRequestDTO request
    ) {
        Route route = createRoute.execute(new CreateRouteCommand(currentUserId(principal), request));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApplicationResponse.success(mapper.toResponse(route)));
    }

    @GetMapping
    public ResponseEntity<ApplicationResponse<List<RouteResponseDTO>>> listMyRoutes(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        List<RouteResponseDTO> routes = listMyRoutes.execute(currentUserId(principal))
                .stream()
                .map(mapper::toResponse)
                .toList();

        return ResponseEntity.ok(ApplicationResponse.success(routes));
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<ApplicationResponse<RouteResponseDTO>> getRoute(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String routeId
    ) {
        Route route = getRoute.execute(new RouteRefCommand(routeId, currentUserId(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(route)));
    }

    @PutMapping("/{routeId}")
    public ResponseEntity<ApplicationResponse<RouteResponseDTO>> updateRoute(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String routeId,
            @Valid @RequestBody UpdateRouteRequestDTO request
    ) {
        Route route = updateRoute.execute(new UpdateRouteCommand(routeId, currentUserId(principal), request));

        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(route)));
    }

    @DeleteMapping("/{routeId}")
    public ResponseEntity<Void> deleteRoute(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String routeId
    ) {
        deleteRoute.execute(new RouteRefCommand(routeId, currentUserId(principal)));

        return ResponseEntity.noContent().build();
    }

    private String currentUserId(AuthenticatedUser principal) {
        return userUtils.getCurrentUser(principal).getId().toHexString();
    }
}
