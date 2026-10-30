package com.converge.api.modules.user.presentation.controller;

import com.converge.api.modules.user.application.CreateVehicleUseCase;
import com.converge.api.modules.user.application.DeleteVehicleUseCase;
import com.converge.api.modules.user.application.GetVehicleUseCase;
import com.converge.api.modules.user.application.ListMyVehiclesUseCase;
import com.converge.api.modules.user.application.UpdateVehicleUseCase;
import com.converge.api.modules.user.application.dto.CreateVehicleCommand;
import com.converge.api.modules.user.application.dto.CreateVehicleRequestDTO;
import com.converge.api.modules.user.application.dto.UpdateVehicleCommand;
import com.converge.api.modules.user.application.dto.UpdateVehicleRequestDTO;
import com.converge.api.modules.user.application.dto.VehicleRefCommand;
import com.converge.api.modules.user.application.dto.VehicleResponseDTO;
import com.converge.api.modules.user.application.mapper.VehicleMapper;
import com.converge.api.modules.user.domain.model.vehicle.Vehicle;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
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
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final CreateVehicleUseCase createVehicle;
    private final ListMyVehiclesUseCase listMyVehicles;
    private final GetVehicleUseCase getVehicle;
    private final UpdateVehicleUseCase updateVehicle;
    private final DeleteVehicleUseCase deleteVehicle;
    private final VehicleMapper mapper;

    @PostMapping
    public ResponseEntity<ApplicationResponse<VehicleResponseDTO>> createVehicle(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateVehicleRequestDTO request
    ) {
        Vehicle vehicle = createVehicle.execute(new CreateVehicleCommand(uid(principal), request));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApplicationResponse.success(mapper.toResponse(vehicle)));
    }

    @GetMapping
    public ResponseEntity<ApplicationResponse<List<VehicleResponseDTO>>> listMyVehicles(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        List<VehicleResponseDTO> vehicles = listMyVehicles.execute(uid(principal))
                .stream()
                .map(mapper::toResponse)
                .toList();

        return ResponseEntity.ok(ApplicationResponse.success(vehicles));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<ApplicationResponse<VehicleResponseDTO>> getVehicle(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String vehicleId
    ) {
        Vehicle vehicle = getVehicle.execute(new VehicleRefCommand(vehicleId, uid(principal)));

        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(vehicle)));
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<ApplicationResponse<VehicleResponseDTO>> updateVehicle(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String vehicleId,
            @Valid @RequestBody UpdateVehicleRequestDTO request
    ) {
        Vehicle vehicle = updateVehicle.execute(new UpdateVehicleCommand(vehicleId, uid(principal), request));

        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(vehicle)));
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String vehicleId
    ) {
        deleteVehicle.execute(new VehicleRefCommand(vehicleId, uid(principal)));

        return ResponseEntity.noContent().build();
    }

    private String uid(AuthenticatedUser principal) {
        if (principal == null || principal.uid() == null) {
            throw new ApplicationException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Não autenticado."
            );
        }

        return principal.uid();
    }
}
