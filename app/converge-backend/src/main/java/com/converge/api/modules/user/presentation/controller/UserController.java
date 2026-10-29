package com.converge.api.modules.user.presentation.controller;

import com.converge.api.modules.user.application.CreateUserUseCase;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.application.dto.CreateUserRequestDTO;
import com.converge.api.modules.user.application.dto.CreateUserResponseDTO;
import com.converge.api.modules.user.application.mapper.UserMapper;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.converge.api.modules.user.application.UpdateUserUseCase;
import com.converge.api.modules.user.application.dto.UpdateUserRequestDTO;
import com.converge.api.modules.user.application.dto.UpdateUserCommand;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUser;
    private final UserUtils userUtils;
    private final UserMapper mapper;
    private final UpdateUserUseCase updateUser;

    @PostMapping
    public ResponseEntity<ApplicationResponse<CreateUserResponseDTO>> createUser(
            @Valid @RequestBody CreateUserRequestDTO request
    ) {
        User user = createUser.execute(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApplicationResponse.success(mapper.toResponse(user)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApplicationResponse<CreateUserResponseDTO>> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        if (principal == null || principal.uid() == null) {
            throw new ApplicationException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Não autenticado."
            );
        }

        User user = userUtils.getCurrentUser(principal);
        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(user)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApplicationResponse<CreateUserResponseDTO>> updateCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateUserRequestDTO request
    ) {
        if (principal == null || principal.uid() == null) {
            throw new ApplicationException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Não autenticado."
            );
        }

        User user = updateUser.execute(new UpdateUserCommand(principal.uid(), request));

        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(user)));
    }
}
