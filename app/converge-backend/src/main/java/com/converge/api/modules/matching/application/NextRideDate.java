// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

import jakarta.annotation.Nullable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Set;

/**
 * Próxima data em que uma combinação acontece: o primeiro dia em comum a partir de hoje,
 * pulando hoje se o motorista já saiu. É a data que o app manda ao pedir a vaga.
 * Os horários "HH:mm" dos trajetos valem no fuso oficial da aplicação.
 */
public final class NextRideDate {

    public static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");

    private static final int DAYS_IN_WEEK = 7;

    private NextRideDate() {
    }

    public static Optional<LocalDate> of(Set<DayOfWeek> days, String departureTime) {
        return of(days, departureTime, LocalDateTime.now(APP_ZONE));
    }

    /**
     * Próxima data da combinação considerando as caronas de um dia só: se um dos trajetos
     * tem data, a viagem é nela (e precisa ser a mesma nos dois, se ambos tiverem).
     */
    public static Optional<LocalDate> of(
            Set<DayOfWeek> days,
            String departureTime,
            @Nullable LocalDate offerDate,
            @Nullable LocalDate searchDate
    ) {
        if (offerDate == null && searchDate == null) {
            return of(days, departureTime);
        }
        if (offerDate != null && searchDate != null && !offerDate.equals(searchDate)) {
            return Optional.empty();
        }

        LocalDate date = offerDate != null ? offerDate : searchDate;
        // Mesma regra do dia a dia: só a partir de hoje e, hoje, só se o motorista ainda não saiu
        return of(Set.of(date.getDayOfWeek()), departureTime)
                .filter(date::equals);
    }

    static Optional<LocalDate> of(Set<DayOfWeek> days, String departureTime, LocalDateTime now) {
        if (days == null || days.isEmpty()) {
            return Optional.empty();
        }

        LocalTime departure;
        try {
            departure = LocalTime.parse(departureTime);
        } catch (DateTimeParseException | NullPointerException e) {
            return Optional.empty();
        }

        // Até 7 dias à frente: hoje pode ser pulado e voltar a ser o mesmo dia da semana
        for (int offset = 0; offset <= DAYS_IN_WEEK; offset++) {
            LocalDate date = now.toLocalDate().plusDays(offset);
            boolean departed = offset == 0 && !departure.isAfter(now.toLocalTime());

            if (days.contains(date.getDayOfWeek()) && !departed) {
                return Optional.of(date);
            }
        }

        return Optional.empty();
    }
}
