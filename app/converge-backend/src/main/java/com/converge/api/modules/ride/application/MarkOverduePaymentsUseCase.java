// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.ride.application;

import com.converge.api.modules.ride.domain.model.Ride;
import com.converge.api.modules.ride.domain.model.subtypes.Participant;
import com.converge.api.modules.ride.domain.repository.RideRepository;
import com.converge.api.modules.user.application.RecordOverduePaymentUseCase;
import com.converge.api.shared.application.UseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Marca como vencidas as cobranças não pagas depois do prazo (RF67). A marcação é atômica:
 * só quem de fato marcou registra a dívida no passageiro.
 * Devolve quantas dívidas foram marcadas.
 */
@Slf4j
@Service
public class MarkOverduePaymentsUseCase implements UseCase<LocalDateTime, Integer> {

    private static final Duration DEFAULT_DUE_AFTER = Duration.ofMinutes(5);

    private final RideRepository rideRepository;
    private final RecordOverduePaymentUseCase recordOverduePayment;
    private final Duration dueAfter;
    private final LocalDateTime overdueSince;

    public MarkOverduePaymentsUseCase(
            RideRepository rideRepository,
            RecordOverduePaymentUseCase recordOverduePayment,
            @Value("${converge.payment.due-after:PT5M}") Duration dueAfter,
            @Value("${converge.payment.overdue-since:2026-11-20T00:00:00}") String overdueSince
    ) {
        this.rideRepository = rideRepository;
        this.recordOverduePayment = recordOverduePayment;
        this.dueAfter = dueAfter != null ? dueAfter : DEFAULT_DUE_AFTER;
        this.overdueSince = parseSince(overdueSince);
    }

    @Override
    public Integer execute(LocalDateTime now) {
        LocalDateTime dueLimit = now.minus(dueAfter);
        List<Ride> rides = rideRepository.findRidesWithDueCharges(dueLimit, overdueSince);

        int marked = 0;
        for (Ride ride : rides) {
            for (Participant participant : ride.getParticipants()) {
                if (!participant.isDueForOverdue(dueLimit, overdueSince)) {
                    continue;
                }
                if (markOne(ride, participant.getUserId(), dueLimit, now)) {
                    marked++;
                }
            }
        }
        return marked;
    }

    private boolean markOne(Ride ride, String passengerId, LocalDateTime dueLimit, LocalDateTime now) {
        try {
            boolean wasMarked = rideRepository
                    .markPaymentOverdueAtomic(ride.getId(), passengerId, dueLimit, overdueSince, now)
                    .isPresent();
            if (wasMarked) {
                recordOverduePayment.execute(passengerId);
            }
            return wasMarked;
        } catch (RuntimeException e) {
            log.warn("Erro ao marcar cobrança vencida do passageiro {} na viagem {}: {}",
                    passengerId, ride.getId(), e.getMessage());
            return false;
        }
    }

    private static LocalDateTime parseSince(String value) {
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "converge.payment.overdue-since inválido. Use o formato 2026-11-20T00:00:00.", e);
        }
    }
}
