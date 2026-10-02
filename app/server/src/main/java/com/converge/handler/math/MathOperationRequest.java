package com.converge.handler.math;

import com.converge.core.protocol.Message;

import java.io.Serial;

public class MathOperationRequest extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final char operation;
    private final double operandA;
    private final double operandB;

    public MathOperationRequest(char operation, double operandA, double operandB) {
        this.operation = operation;
        this.operandA = operandA;
        this.operandB = operandB;
    }

    public char getOperation() {
        return operation;
    }

    public double getOperandA() {
        return operandA;
    }

    public double getOperandB() {
        return operandB;
    }

    @Override
    public String toString() {
        return "MathOperationRequest{" + operandA + " " + operation + " " + operandB + "}";
    }
}
