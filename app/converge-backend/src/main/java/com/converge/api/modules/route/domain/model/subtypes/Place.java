package com.converge.api.modules.route.domain.model.subtypes;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;

public record Place(

        @NotBlank
        String address,

        @Nullable
        String neighborhood,

        @NotBlank
        String city,

        @NotNull
        @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
        GeoJsonPoint location
) {

    public static Place of(String address, @Nullable String neighborhood, String city, Coordinates coordinates) {
        // GeoJsonPoint(x, y): x = longitude, y = latitude.
        GeoJsonPoint location = new GeoJsonPoint(coordinates.longitude(), coordinates.latitude());

        return new Place(address, neighborhood, city, location);
    }

    public Coordinates coordinates() {
        // GeoJSON guarda [longitude, latitude]: x = longitude, y = latitude
        return new Coordinates(location.getY(), location.getX());
    }
}
