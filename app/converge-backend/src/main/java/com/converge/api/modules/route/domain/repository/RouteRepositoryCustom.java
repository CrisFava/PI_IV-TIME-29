// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.route.domain.repository;

import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;

public interface RouteRepositoryCustom {

    // Trajetos ACTIVE do tipo, com um dia em comum e com origem e destino dentro do raio (índices 2dsphere)
    List<Route> findActiveNear(
            RouteType type,
            Collection<DayOfWeek> weekDays,
            Coordinates origin,
            Coordinates destination,
            double radiusKm
    );
}
