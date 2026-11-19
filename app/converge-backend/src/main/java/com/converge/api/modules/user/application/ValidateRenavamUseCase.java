// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.user.application;

import com.converge.api.modules.user.application.gateway.RenavamValidationGateway;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Confere o RENAVAM de um veículo e devolve só os dígitos. Recusa com o mesmo erro que
 * o cadastro de veículo já devolvia antes de a regra ir para o servidor.
 */
@Service
@RequiredArgsConstructor
public class ValidateRenavamUseCase implements UseCase<String, String> {

    private final RenavamValidationGateway gateway;

    @Override
    public String execute(String renavam) {
        RenavamValidationGateway.Result result = gateway.validate(renavam);
        if (!result.valid()) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, result.message(), Set.of("renavam"));
        }
        return result.normalizedRenavam();
    }
}
