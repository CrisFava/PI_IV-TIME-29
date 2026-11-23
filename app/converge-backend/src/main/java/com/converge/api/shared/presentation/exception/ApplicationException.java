// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.presentation.exception;

import com.converge.api.shared.infrastructure.exception.ErrorResponse;
import lombok.Getter;

import java.util.Set;

/**
 * Exceção padrão de erros de negócio da API.
 *
 * É lançada pelos casos de uso, entidades e controllers quando uma regra é
 * violada (ex.: NOT_ALLOWED, RIDE_NOT_FOUND, VEHICLE_ALREADY_EXISTS). Carrega um
 * ErrorResponse (código, mensagem e, opcionalmente, os campos envolvidos), que o
 * GlobalExceptionHandler converte em resposta HTTP, usando o status definido no
 * ErrorCode. Por ser RuntimeException, não precisa ser declarada com "throws".
 */
@Getter
public class ApplicationException extends RuntimeException {

    private final ErrorResponse error;

    /**
     * Cria o erro com código e mensagem, sem indicar campos específicos.
     * A mesma mensagem vai para a exceção (log/stack trace) e para o ErrorResponse.
     */
    public ApplicationException(ErrorCode code,  String message){
        super(message);
        this.error = new ErrorResponse(code, message);
    }

    /**
     * Cria o erro com código, mensagem e o conjunto de campos que causaram o
     * problema (ex.: Set.of("licensePlate")), para o app destacar os campos
     * certos no formulário.
     */
    public ApplicationException(ErrorCode code, String message, Set<String> fields){
        super(message);
        this.error = new ErrorResponse(code, message, fields);
    }
}
