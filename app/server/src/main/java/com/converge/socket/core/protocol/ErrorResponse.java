// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.protocol;

import com.converge.socket.core.exception.ServerErrorCode;

import java.io.Serial;

/**
 * Mensagem de protocolo retornada pelo servidor para sinalizar falhas ou erros durante o processamento de requisições.
 * Encapsula o código padronizado do erro e a respectiva mensagem explicativa.
 */
public class ErrorResponse extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String errorMessage;

    /**
     * Cria uma resposta de erro a partir de um {@link ServerErrorCode}, utilizando sua mensagem padrão.
     *
     * @param errorCode enum com o código e mensagem padrão do erro
     */
    public ErrorResponse(ServerErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getDefaultMessage());
    }

    /**
     * Cria uma resposta de erro a partir de um {@link ServerErrorCode} associando uma mensagem descritiva customizada.
     *
     * @param errorCode enum com o código do erro
     * @param errorMessage mensagem explicativa detalhada do erro
     */
    public ErrorResponse(ServerErrorCode errorCode, String errorMessage) {
        this(errorCode.getCode(), errorMessage);
    }

    /**
     * Cria uma resposta de erro a partir do código textual e da mensagem de erro.
     *
     * @param errorCode código textual identificador do erro
     * @param errorMessage mensagem explicativa do erro
     */
    public ErrorResponse(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    // Getters e setters

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Retorna a representação textual estruturada da resposta de erro.
     *
     * @return string descritiva da resposta de erro
     */
    @Override
    public String toString() {
        return "ErrorResponse{errorCode='" + errorCode + "', errorMessage='" + errorMessage + "'}";
    }
}
