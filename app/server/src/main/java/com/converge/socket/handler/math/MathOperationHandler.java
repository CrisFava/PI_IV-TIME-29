package com.converge.socket.handler.math;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.RequestHandler;


// Use ServerException caso for lançar erros para manter a semântica.
public class MathOperationHandler implements RequestHandler<MathOperationRequest, MathOperationResponse> {

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

    private double divide(double a, double b) throws ServerException {
        if (b == 0.0) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Division by zero is not allowed");
        }

        return a / b;
    }
}
