// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.socket.handler.math;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.RequestHandler;


/**
 * Handler responsável por executar operações matemáticas aritméticas básicas (+, -, *, /).
 */
public class MathOperationHandler implements RequestHandler<MathOperationRequest, MathOperationResponse> {

    /**
     * Executa a operação matemática aritmética solicitada na requisição.
     *
     * @param request requisição contendo o operador e os dois operandos numéricos
     * @return resposta com o resultado do cálculo
     * @throws ServerException se a requisição for nula, a operação for desconhecida ou houver divisão por zero
     */
    @Override
    public MathOperationResponse execute(MathOperationRequest request) throws ServerException {
        if (request == null) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Request cannot be null");
        }

        double a = request.getOperandA();
        double b = request.getOperandB();

        double result = switch (request.getOperation()) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> divide(a, b);
            default -> throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Unknown operation: " + request.getOperation());
        };

        return new MathOperationResponse(result);
    }

    /**
     * Realiza a divisão entre dois números, validando contra divisão por zero.
     *
     * @param a dividendo
     * @param b divisor
     * @return quociente da divisão
     * @throws ServerException se o divisor for zero
     */
    private double divide(double a, double b) throws ServerException {
        if (b == 0.0) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Division by zero is not allowed");
        }

        return a / b;
    }
}
