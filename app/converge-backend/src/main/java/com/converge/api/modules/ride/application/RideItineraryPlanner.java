// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStopType;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.model.subtypes.RideStop;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.api.modules.route.domain.model.subtypes.Place;
import com.converge.api.modules.route.domain.service.HaversineCalculator;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Ordem das paradas da viagem: o motorista sai da origem, busca cada passageiro
 * confirmado onde ele embarca e depois deixa cada um onde desembarca, antes de
 * seguir para o destino. Em cada fase, a próxima parada é a mais próxima da atual.
 * Buscar todos antes de deixar garante que ninguém é deixado antes de embarcar.
 */
@Component
public class RideItineraryPlanner {

    public List<RideStop> plan(Ride ride) {
        List<Participant> passengers = ride.getParticipants().stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .filter(participant -> participant.getStatus() == ParticipantStatus.CONFIRMED)
                .filter(participant -> participant.getBoardingLocation() != null
                        && participant.getDropOffLocation() != null)
                .toList();

        List<RideStop> stops = new ArrayList<>();
        Coordinates current = ride.getOrigin().coordinates();
        current = appendNearestFirst(stops, passengers, current, RideStopType.PICKUP, Participant::getBoardingLocation);
        appendNearestFirst(stops, passengers, current, RideStopType.DROPOFF, Participant::getDropOffLocation);
        return stops;
    }

    // Acrescenta uma parada por passageiro, sempre a mais próxima de onde o carro está; devolve onde ele termina
    private static Coordinates appendNearestFirst(
            List<RideStop> stops,
            List<Participant> passengers,
            Coordinates start,
            RideStopType type,
            Function<Participant, Place> placeOf
    ) {
        List<Participant> remaining = new ArrayList<>(passengers);
        Coordinates current = start;

        while (!remaining.isEmpty()) {
            Coordinates from = current;
            Participant next = remaining.stream()
                    .min(Comparator
                            .comparingDouble((Participant p) ->
                                    HaversineCalculator.distanceKm(from, placeOf.apply(p).coordinates()))
                            .thenComparing(Participant::getUserId))
                    .orElseThrow();

            Place place = placeOf.apply(next);
            stops.add(new RideStop(type, next.getUserId(), place));
            current = place.coordinates();
            remaining.remove(next);
        }
        return current;
    }
}
