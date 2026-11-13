// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.api.modules.matching.application;

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
