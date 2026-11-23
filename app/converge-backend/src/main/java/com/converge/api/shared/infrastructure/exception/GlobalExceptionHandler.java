// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.exception;

import com.converge.api.shared.presentation.dto.ApplicationResponse;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tratador global de exceções da API (@RestControllerAdvice).
 *
 * Intercepta as exceções lançadas pelos controllers e casos de uso e as converte
 * em respostas HTTP no formato padrão ApplicationResponse (falha com código e
 * mensagem), para o app receber sempre a mesma estrutura de erro. Cada método
 * cuida de um tipo de exceção.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata os erros de negócio (ApplicationException), que são os mais comuns:
     * usa o ErrorResponse da própria exceção e o status HTTP definido no ErrorCode
     * (ex.: NOT_ALLOWED, RIDE_NOT_FOUND, VEHICLE_ALREADY_EXISTS).
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApplicationResponse<Void>> handle(
            ApplicationException ex
    ) {
        ErrorResponse error = ex.getError();

        return ResponseEntity
                .status(error.code().getStatus())
                .body(ApplicationResponse.failure(error));
    }

    /**
     * Trata falhas de validação do corpo da requisição (@Valid nos DTOs).
     * Devolve BAD_REQUEST com a lista dos campos inválidos e, como mensagem, a
     * do primeiro erro encontrado (ou um texto genérico se não houver nenhum).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApplicationResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        Set<String> fields = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getField)
                .collect(Collectors.toSet());

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Erro de validação nos campos informados.");

        ErrorResponse error = new ErrorResponse(ErrorCode.BAD_REQUEST, message, fields);

        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getStatus())
                .body(ApplicationResponse.failure(error));
    }

    /**
     * Trata violação de índice único do MongoDB (DuplicateKeyException) que
     * escapou dos casos de uso. Sempre responde USER_ALREADY_EXISTS com a
     * mensagem "Já existe um usuário cadastrado com estes dados."
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApplicationResponse<Void>> handleDuplicateKey(
            DuplicateKeyException ex
    ) {
        ErrorResponse error = new ErrorResponse(
                ErrorCode.USER_ALREADY_EXISTS,
                "Já existe um usuário cadastrado com estes dados."
        );

        return ResponseEntity
                .status(ErrorCode.USER_ALREADY_EXISTS.getStatus())
                .body(ApplicationResponse.failure(error));
    }

    /**
     * Trata corpo de requisição ilegível (JSON malformado, tipo errado, enum
     * inválido etc.). Devolve BAD_REQUEST com a mensagem da própria exceção.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApplicationResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex
    ) {
        ErrorResponse error = new ErrorResponse(
                ErrorCode.BAD_REQUEST,
                ex.getMessage()
        );

        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getStatus())
                .body(ApplicationResponse.failure(error));
    }
}