// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.modules.user.presentation.controller;

import com.converge.api.modules.user.application.CheckEmailAvailabilityUseCase;
import com.converge.api.modules.user.application.CreateUserUseCase;
import com.converge.api.modules.user.application.dto.*;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.modules.user.application.mapper.UserMapper;
import com.converge.api.modules.user.application.ValidateCpfUseCase;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.converge.api.modules.user.application.UpdateUserUseCase;
import org.springframework.web.bind.annotation.PutMapping;
import com.converge.api.modules.user.application.DeleteUserUseCase;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.converge.api.modules.user.application.BecomeDriverUseCase;
import com.converge.api.modules.user.application.SubmitDriverLicenseUseCase;


/**
 * Controller REST dos usuários (/api/v1/users).
 *
 * Camada de entrada da API: recebe a requisição HTTP, descobre o usuário logado
 * (UID do Firebase, quando o endpoint exige autenticação) e delega a regra de
 * negócio ao caso de uso correspondente. A resposta é convertida em
 * CreateUserResponseDTO pelo UserMapper. Cobre cadastro, consulta, edição e
 * exclusão do próprio usuário, a ida para o papel de motorista, o envio da CNH
 * e validações auxiliares (e-mail e CPF). Não há regra de negócio aqui.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUser;
    private final UserUtils userUtils;
    private final UserMapper mapper;
    private final UpdateUserUseCase updateUser;
    private final DeleteUserUseCase deleteUser;
    private final BecomeDriverUseCase becomeDriver;
    private final SubmitDriverLicenseUseCase submitDriverLicense;
    private final ValidateCpfUseCase validateCpf;
    private final CheckEmailAvailabilityUseCase checkEmailAvailability;

    /**
     * GET /email-availability?email=...: diz se um e-mail ainda está livre para
     * cadastro. Não exige usuário logado, porque é usado antes do cadastro.
     */
    @GetMapping("/email-availability")
    public ResponseEntity<ApplicationResponse<EmailAvailabilityResponseDTO>> checkEmailAvailability(
            @RequestParam String email
    ) {
        return ResponseEntity.ok(
                ApplicationResponse.success(checkEmailAvailability.execute(email))
        );
    }

    /**
     * POST /: cadastra um novo usuário. Retorna 201 (CREATED) com os dados do
     * usuário criado. Não usa @AuthenticationPrincipal: o firebaseUid vem no
     * próprio corpo da requisição (CreateUserRequestDTO).
     */
    @PostMapping
    public ResponseEntity<ApplicationResponse<CreateUserResponseDTO>> createUser(
            @Valid @RequestBody CreateUserRequestDTO request
    ) {
        User user = createUser.execute(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApplicationResponse.success(mapper.toResponse(user)));
    }

    /**
     * GET /me: devolve os dados do usuário logado. Lança INVALID_CREDENTIALS
     * ("Não autenticado.") se não houver usuário autenticado ou UID.
     */
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

    /**
     * PUT /me: atualiza os dados do usuário logado (perfil e PIX) e devolve o
     * usuário atualizado. Exige autenticação.
     */
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


    /**
     * DELETE /me: exclui a conta do usuário logado. Retorna 204 (sem conteúdo).
     * Pela entidade User, a exclusão deve ser lógica (deactivate), mas quem
     * decide isso é o DeleteUserUseCase.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser(

            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        if (principal == null || principal.uid() == null) {
            throw new ApplicationException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Não autenticado."
            );
        }
        deleteUser.execute(principal.uid());

        return ResponseEntity.noContent().build();
    }


    /**
     * POST /me/driver: o usuário logado pede para virar motorista (adiciona o
     * papel DRIVER). Os requisitos (CNH e PIX) são conferidos no domínio.
     */
    @PostMapping("/me/driver")
    public ResponseEntity<ApplicationResponse<CreateUserResponseDTO>> becomeDriver(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        if (principal == null || principal.uid() == null) {
            throw new ApplicationException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Não autenticado."
            );
        }
        User user = becomeDriver.execute(principal.uid());
        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(user)));
    }

    /**
     * POST /me/driver-license: o usuário logado envia os dados da CNH (número,
     * categoria, validade e documento). Ela entra em análise (UNDER_REVIEW).
     */
    @PostMapping("/me/driver-license")
    public ResponseEntity<ApplicationResponse<CreateUserResponseDTO>> submitDriverLicense(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody DriverLicenseRequestDTO request
    ) {
        if (principal == null || principal.uid() == null) {
            throw new ApplicationException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Não autenticado."
            );
        }
        User user = submitDriverLicense.execute(new SubmitDriverLicenseCommand(principal.uid(), request));

        return ResponseEntity.ok(ApplicationResponse.success(mapper.toResponse(user)));
    }

    /**
     * POST /validate-cpf: valida um CPF recebido no corpo e devolve o resultado.
     * Não exige usuário logado.
     */
    @PostMapping("/validate-cpf")
    public ResponseEntity<ApplicationResponse<CpfValidationResponseDTO>> validateCpf(
            @Valid @RequestBody CpfValidationRequestDTO request
    ) {
        CpfValidationResponseDTO result = validateCpf.execute(request);
        return ResponseEntity.ok(ApplicationResponse.success(result));
    }
}
