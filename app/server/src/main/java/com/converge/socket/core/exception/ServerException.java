// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.core.exception;

/**
 * Exceção personalizada para operações da camada de sockets do servidor.
 * Encapsula um {@link ServerErrorCode} e fornece contexto detalhado sobre falhas de rede, protocolo e I/O.
 */
public class ServerException extends Exception {

    private final ServerErrorCode errorCode;

    /**
     * Cria uma nova exceção baseada apenas no código de erro, adotando sua mensagem padrão.
     *
     * @param errorCode código de erro padronizado do servidor
     */
    public ServerException(ServerErrorCode errorCode) {
        super(errorCode != null ? errorCode.getDefaultMessage() : "Unknown server error");
        this.errorCode = errorCode != null ? errorCode : ServerErrorCode.INTERNAL_SERVER_ERROR;
    }

    /**
     * Cria uma nova exceção especificando o código de erro e uma mensagem detalhada.
     *
     * @param errorCode código de erro padronizado do servidor
     * @param message mensagem descritiva da falha
     */
    public ServerException(ServerErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : ServerErrorCode.INTERNAL_SERVER_ERROR;
    }

    /**
     * Cria uma nova exceção com código de erro, mensagem detalhada e causa raiz encadeada.
     *
     * @param errorCode código de erro padronizado do servidor
     * @param message mensagem descritiva da falha
     * @param cause exceção original causadora do erro
     */
    public ServerException(ServerErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : ServerErrorCode.INTERNAL_SERVER_ERROR;
    }

    /**
     * Cria uma nova exceção com código de erro e causa raiz encadeada.
     *
     * @param errorCode código de erro padronizado do servidor
     * @param cause exceção original causadora do erro
     */
    public ServerException(ServerErrorCode errorCode, Throwable cause) {
        super(errorCode != null ? errorCode.getDefaultMessage() : "Unknown server error", cause);
        this.errorCode = errorCode != null ? errorCode : ServerErrorCode.INTERNAL_SERVER_ERROR;
    }

    // Getters e setters

    public ServerErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorCodeString() {
        return errorCode.getCode();
    }

    /**
     * Retorna a mensagem descritiva do erro prefixada pelo código entre colchetes.
     *
     * @return mensagem formatada com o código do erro
     */
    @Override
    public String getMessage() {
        return "[" + errorCode.getCode() + "] " + super.getMessage();
    }
}
