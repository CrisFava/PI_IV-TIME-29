package com.converge.handler.math;

import com.converge.core.protocol.Message;

import java.io.Serial;

public class MathOperationResponse extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final double result;

    public MathOperationResponse(double result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "MathOperationResponse{result=" + result + "}";
    }
}
