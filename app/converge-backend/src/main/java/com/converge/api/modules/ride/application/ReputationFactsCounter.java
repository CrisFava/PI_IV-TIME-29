// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.application.dto.ReputationFactsDTO;
import com.converge.api.modules.ride.domain.enums.ParticipantStatus;
import com.converge.api.modules.ride.domain.enums.RideStatus;
import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.user.domain.enums.user.ParticipationRole;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Conta, por usuário, os fatos de reputação das viagens com data entre [since] e [now].
 * Segue as mesmas regras dos incrementos em tempo real. Um fato com exatamente [since]
 * ainda conta.
 */
@Component
public class ReputationFactsCounter {

    public Map<String, ReputationFactsDTO> count(Stream<Ride> rides, LocalDateTime since, LocalDateTime now) {
        Map<String, Tally> tallies = new HashMap<>();
        rides.forEach(ride -> countRide(ride, since, now, tallies));

        Map<String, ReputationFactsDTO> facts = new HashMap<>();
        tallies.forEach((userId, tally) -> facts.put(userId, tally.toDto()));
        return facts;
    }

    private void countRide(Ride ride, LocalDateTime since, LocalDateTime now, Map<String, Tally> tallies) {
        List<Participant> passengers = ride.getParticipants().stream()
                .filter(participant -> participant.getRole() == ParticipationRole.PASSENGER)
                .toList();

        // Viagem concluída só conta com passageiro efetivo (RF49)
        if (ride.getStatus() == RideStatus.COMPLETED && inWindow(ride.getCompletedAt(), since, now)) {
            List<Participant> completed = passengers.stream()
                    .filter(participant -> participant.getStatus() == ParticipantStatus.COMPLETED)
                    .toList();
            if (!completed.isEmpty()) {
                tally(tallies, ride.getDriverId()).driverCompletedRides++;
                completed.forEach(participant -> tally(tallies, participant.getUserId()).passengerCompletedRides++);
            }
        }

        // A ausência não tem data própria: usa o início da viagem
        if (inWindow(ride.getStartedAt(), since, now)) {
            passengers.stream()
                    .filter(participant -> participant.getStatus() == ParticipantStatus.NO_SHOW)
                    .forEach(participant -> tally(tallies, participant.getUserId()).passengerNoShows++);
        }

        for (Participant passenger : passengers) {
            if (passenger.getStatus() == ParticipantStatus.CANCELLED
                    && passenger.isLateCancellation()
                    && inWindow(passenger.getCancelledAt(), since, now)) {
                tally(tallies, passenger.getUserId()).passengerLateCancellations++;
            }
            if (inWindow(passenger.getOverdueAt(), since, now)) {
                tally(tallies, passenger.getUserId()).passengerOverdueRides++;
            }
            // Remoção de passageiro confirmado também penaliza o motorista
            if (passenger.isRemovedByDriver() && inWindow(passenger.getCancelledAt(), since, now)) {
                tally(tallies, ride.getDriverId()).driverCancellations++;
            }
        }

        if (ride.isDriverCancellationPenalized() && inWindow(ride.getCancelledAt(), since, now)) {
            tally(tallies, ride.getDriverId()).driverCancellations++;
        }
    }

    private boolean inWindow(LocalDateTime moment, LocalDateTime since, LocalDateTime now) {
        return moment != null && !moment.isBefore(since) && !moment.isAfter(now);
    }

    private Tally tally(Map<String, Tally> tallies, String userId) {
        return tallies.computeIfAbsent(userId, id -> new Tally());
    }

    private static final class Tally {
        private int driverCompletedRides;
        private int driverCancellations;
        private int passengerCompletedRides;
        private int passengerLateCancellations;
        private int passengerNoShows;
        private int passengerOverdueRides;

        private ReputationFactsDTO toDto() {
            return new ReputationFactsDTO(
                    driverCompletedRides,
                    driverCancellations,
                    passengerCompletedRides,
                    passengerLateCancellations,
                    passengerNoShows,
                    passengerOverdueRides
            );
        }
    }
}
