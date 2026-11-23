// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.domain.model;

import com.converge.api.modules.security.domain.enums.ReportReason;
import com.converge.api.modules.security.domain.enums.ReportStatus;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes unitários para {@link Report}.
 * Valida cenários como: Denúncia nova nasce PENDING, sem resolução nem moderador; Viagem e descrição são opcionais para motivos comuns e vazios viram nulos; Não pode denunciar a si mesmo.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class ReportTest {

    private static void assertBadRequest(Runnable action, String field) {
        ApplicationException ex = assertThrows(ApplicationException.class, action::run);
        assertThat(ex.getError().code()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ex.getError().fields()).contains(field);
    }

    @Test
    @DisplayName("Denúncia nova nasce PENDING, sem resolução nem moderador")
    void shouldStartPending() {
        Report report = new Report("reporter", "reported", "ride-1", ReportReason.INAPPROPRIATE_CONDUCT, "Comentários ofensivos");

        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(report.getReporterId()).isEqualTo("reporter");
        assertThat(report.getReportedId()).isEqualTo("reported");
        assertThat(report.getRideId()).isEqualTo("ride-1");
        assertThat(report.getReason()).isEqualTo(ReportReason.INAPPROPRIATE_CONDUCT);
        assertThat(report.getDescription()).isEqualTo("Comentários ofensivos");
        assertThat(report.getResolution()).isNull();
        assertThat(report.getReviewedBy()).isNull();
    }

    @Test
    @DisplayName("Viagem e descrição são opcionais para motivos comuns e vazios viram nulos")
    void shouldAcceptMissingRideAndDescription() {
        Report report = new Report("reporter", "reported", " ", ReportReason.IMPROPER_CHARGE, "  ");

        assertThat(report.getRideId()).isNull();
        assertThat(report.getDescription()).isNull();
    }

    @Test
    @DisplayName("Não pode denunciar a si mesmo")
    void shouldRejectSelfReport() {
        assertBadRequest(
                () -> new Report("same", "same", null, ReportReason.IMPROPER_CHARGE, null), "reportedId");
    }

    @Test
    @DisplayName("Quem denuncia, denunciado e motivo são obrigatórios")
    void shouldRequireMandatoryFields() {
        assertBadRequest(() -> new Report(null, "reported", null, ReportReason.IMPROPER_CHARGE, null), "reporterId");
        assertBadRequest(() -> new Report("reporter", " ", null, ReportReason.IMPROPER_CHARGE, null), "reportedId");
        assertBadRequest(() -> new Report("reporter", "reported", null, null, null), "reason");
    }

    @Test
    @DisplayName("Atraso ou ausência exige a viagem (RF25)")
    void shouldRequireRideForLateNoShow() {
        assertBadRequest(() -> new Report("reporter", "reported", null, ReportReason.LATE_NO_SHOW, null), "rideId");
        assertBadRequest(() -> new Report("reporter", "reported", "  ", ReportReason.LATE_NO_SHOW, null), "rideId");

        Report report = new Report("reporter", "reported", "ride-1", ReportReason.LATE_NO_SHOW, null);
        assertThat(report.getRideId()).isEqualTo("ride-1");
    }

    @Test
    @DisplayName("Falta de pagamento exige a viagem (RF67)")
    void shouldRequireRideForNonPayment() {
        assertBadRequest(() -> new Report("reporter", "reported", null, ReportReason.NON_PAYMENT, null), "rideId");
        assertBadRequest(() -> new Report("reporter", "reported", "  ", ReportReason.NON_PAYMENT, null), "rideId");

        Report report = new Report("reporter", "reported", "ride-1", ReportReason.NON_PAYMENT, null);

        assertThat(report.getRideId()).isEqualTo("ride-1");
    }

    @Test
    @DisplayName("Motivo OUTRO exige descrição")
    void shouldRequireDescriptionForOther() {
        assertBadRequest(() -> new Report("reporter", "reported", null, ReportReason.OTHER, null), "description");
        assertBadRequest(() -> new Report("reporter", "reported", null, ReportReason.OTHER, "   "), "description");
    }

    @Test
    @DisplayName("Descrição aceita até 500 caracteres")
    void shouldLimitDescriptionLength() {
        Report atLimit = new Report("reporter", "reported", null, ReportReason.OTHER, "a".repeat(500));
        assertThat(atLimit.getDescription()).hasSize(500);

        assertBadRequest(
                () -> new Report("reporter", "reported", null, ReportReason.OTHER, "a".repeat(501)), "description");
    }

    @Test
    @DisplayName("Descrição é guardada sem espaços nas pontas e o limite conta o texto sem eles")
    void shouldTrimDescriptionBeforeStoringAndCounting() {
        Report trimmed = new Report("reporter", "reported", null, ReportReason.OTHER, "  texto  ");
        Report atLimitWithSpaces = new Report(
                "reporter", "reported", null, ReportReason.OTHER, "  " + "a".repeat(500) + "  ");

        assertThat(trimmed.getDescription()).isEqualTo("texto");
        assertThat(atLimitWithSpaces.getDescription()).hasSize(500);
    }
}
