// Autor:

package com.converge.socket.handler.ride;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.handler.RequestHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler responsável pelos cálculos matemáticos de desvio, valor teto e rateio da viagem.
 */
public class RideCostHandler implements RequestHandler<RideCostRequest, RideCostResponse> {

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    @Override
    public RideCostResponse execute(RideCostRequest request) throws ServerException {
        validateRequest(request);

        return RideCostResponse.forDefault();
    }

    private void validateRequest(RideCostRequest request) throws ServerException {
        if (request == null || request.getOperation() == null) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "RideCostRequest cannot be null");
        }
        if (!Double.isFinite(request.getAverageConsumption())
                || request.getAverageConsumption() <= 0.0
                || !Double.isFinite(request.getBaseDistanceKm())
                || request.getBaseDistanceKm() < 0.0
                || request.getCurrentEnergyPrice() == null) {
            throw new ServerException(ServerErrorCode.INVALID_MESSAGE, "Valid fuel consumption, base distance, and energy price are required");
        }
    }
}
