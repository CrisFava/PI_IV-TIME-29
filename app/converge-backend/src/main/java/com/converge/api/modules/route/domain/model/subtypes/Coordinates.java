package com.converge.api.modules.route.domain.model.subtypes;

import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;

import java.util.Set;

public record Coordinates(double latitude, double longitude) {

    private static final double MAX_LATITUDE = 90.0;
    private static final double MAX_LONGITUDE = 180.0;

    public Coordinates {
        if (latitude > MAX_LATITUDE || latitude < -MAX_LATITUDE) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Invalid latitude. Value must be between -90 and 90 degrees.",
                    Set.of("latitude")
            );
        }

        if (longitude > MAX_LONGITUDE || longitude < -MAX_LONGITUDE) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Invalid longitude. Value must be between -180 and 180 degrees.",
                    Set.of("longitude")
            );
        }
    }
}
