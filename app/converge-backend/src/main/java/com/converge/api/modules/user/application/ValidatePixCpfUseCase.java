// Autor: Sara Fernandes Monteiro - RA 25024107

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.CpfValidationGateway;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Confere o CPF da chave PIX no servidor e devolve só os dígitos.
 */
@Service
@RequiredArgsConstructor
public class ValidatePixCpfUseCase implements UseCase<String, String> {

    private final CpfValidationGateway gateway;

    @Override
    public String execute(String cpf) {
        CpfValidationGateway.Result result = gateway.validate(cpf);
        if (!result.valid()) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, result.message(), Set.of("pixKey"));
        }
        return result.formattedCpf().replaceAll("\\D", "");
    }
}
