// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application;

import com.converge.api.modules.security.application.dto.CreateReportRequestDTO;
import com.converge.api.modules.security.application.gateway.PaymentDebtGateway;
import com.converge.api.modules.security.application.gateway.RideParticipationLookupGateway;
import com.converge.api.modules.security.application.gateway.RideParticipationLookupGateway.RideParticipants;
import com.converge.api.modules.security.application.gateway.UserLookupGateway;
import com.converge.api.modules.security.domain.enums.ReportReason;
import com.converge.api.modules.security.domain.enums.ReportStatus;
import com.converge.api.modules.security.domain.model.Report;
import com.converge.api.modules.security.domain.repository.ReportRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link CreateReportUseCase}.
 * Valida cenários como: Deve registrar a denúncia como PENDING com o autor vindo do usuário logado; Não deve aceitar denúncia contra si mesmo; Não deve aceitar denúncia contra usuário inexistente.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
@ExtendWith(MockitoExtension.class)
class CreateReportUseCaseTest {

    private static final String RIDE_ID = "ride-1";
    private static final String DRIVER = "driver-1";
    private static final String PASSENGER = "passenger-1";
    private static final String STRANGER = "stranger-1";

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserLookupGateway userLookup;

    @Mock
    private RideParticipationLookupGateway rideLookup;

    @Mock
    private PaymentDebtGateway paymentDebt;

    private CreateReportUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateReportUseCase(reportRepository, userLookup, rideLookup, paymentDebt);
    }

    private static CreateReportRequestDTO request(String reportedId, ReportReason reason, String description, String rideId) {
        return new CreateReportRequestDTO(reportedId, reason, description, rideId);
    }

    private void rideWithDriverAndPassenger() {
        when(rideLookup.findParticipants(RIDE_ID))
                .thenReturn(Optional.of(new RideParticipants(RIDE_ID, DRIVER, Set.of(PASSENGER))));
    }

    private void assertFails(ErrorCode expected, CreateReportUseCase.Command command) {
        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.execute(command));
        assertThat(ex.getError().code()).isEqualTo(expected);
        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve registrar a denúncia como PENDING com o autor vindo do usuário logado")
    void shouldCreatePendingReport() {
        when(userLookup.exists(DRIVER)).thenReturn(true);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Report report = useCase.execute(new CreateReportUseCase.Command(
                PASSENGER, request(DRIVER, ReportReason.IMPROPER_CHARGE, "Cobrou a mais", null)));

        assertThat(report.getReporterId()).isEqualTo(PASSENGER);
        assertThat(report.getReportedId()).isEqualTo(DRIVER);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(report.getRideId()).isNull();
        verifyNoInteractions(rideLookup);
    }

    @Test
    @DisplayName("Não deve aceitar denúncia contra si mesmo")
    void shouldRejectSelfReport() {
        assertFails(ErrorCode.BAD_REQUEST, new CreateReportUseCase.Command(
                PASSENGER, request(PASSENGER, ReportReason.IMPROPER_CHARGE, null, null)));
        verifyNoInteractions(userLookup);
    }

    @Test
    @DisplayName("Não deve aceitar denúncia contra usuário inexistente")
    void shouldRejectUnknownReportedUser() {
        when(userLookup.exists(DRIVER)).thenReturn(false);

        assertFails(ErrorCode.USER_NOT_FOUND, new CreateReportUseCase.Command(
                PASSENGER, request(DRIVER, ReportReason.IMPROPER_CHARGE, null, null)));
    }

    @Test
    @DisplayName("Atraso ou ausência com a viagem em comum deve ser aceito (RF25)")
    void shouldAcceptLateNoShowWhenBothWereInTheRide() {
        when(userLookup.exists(PASSENGER)).thenReturn(true);
        rideWithDriverAndPassenger();
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Motorista denuncia o passageiro ausente
        Report report = useCase.execute(new CreateReportUseCase.Command(
                DRIVER, request(PASSENGER, ReportReason.LATE_NO_SHOW, null, RIDE_ID)));

        assertThat(report.getRideId()).isEqualTo(RIDE_ID);
        assertThat(report.getReason()).isEqualTo(ReportReason.LATE_NO_SHOW);
    }

    @Test
    @DisplayName("Atraso ou ausência sem a viagem deve ser recusado")
    void shouldRejectLateNoShowWithoutRide() {
        assertFails(ErrorCode.BAD_REQUEST, new CreateReportUseCase.Command(
                DRIVER, request(PASSENGER, ReportReason.LATE_NO_SHOW, null, null)));
    }

    @Test
    @DisplayName("Viagem inexistente deve ser recusada")
    void shouldRejectUnknownRide() {
        when(userLookup.exists(PASSENGER)).thenReturn(true);
        when(rideLookup.findParticipants(RIDE_ID)).thenReturn(Optional.empty());

        assertFails(ErrorCode.RIDE_NOT_FOUND, new CreateReportUseCase.Command(
                DRIVER, request(PASSENGER, ReportReason.LATE_NO_SHOW, null, RIDE_ID)));
    }

    @Test
    @DisplayName("Quem denuncia precisa ter participado da viagem informada")
    void shouldRejectWhenReporterWasNotInTheRide() {
        when(userLookup.exists(PASSENGER)).thenReturn(true);
        rideWithDriverAndPassenger();

        assertFails(ErrorCode.NOT_ALLOWED, new CreateReportUseCase.Command(
                STRANGER, request(PASSENGER, ReportReason.INAPPROPRIATE_CONDUCT, null, RIDE_ID)));
    }

    @Test
    @DisplayName("O denunciado precisa ter participado da viagem informada")
    void shouldRejectWhenReportedWasNotInTheRide() {
        when(userLookup.exists(STRANGER)).thenReturn(true);
        rideWithDriverAndPassenger();

        assertFails(ErrorCode.NOT_ALLOWED, new CreateReportUseCase.Command(
                DRIVER, request(STRANGER, ReportReason.LATE_NO_SHOW, null, RIDE_ID)));
    }

    private void rideWithChargedPassenger() {
        when(rideLookup.findParticipants(RIDE_ID)).thenReturn(Optional.of(
                new RideParticipants(RIDE_ID, DRIVER, Set.of(PASSENGER), Set.of(PASSENGER), Set.of(PASSENGER))));
    }

    @Test
    @DisplayName("Cobrança sem \"Já paguei\" não pode ser denunciada, nem dentro nem fora do prazo")
    void shouldRejectNonPaymentWhileChargeIsPendingWithinDeadline() {
        when(userLookup.exists(PASSENGER)).thenReturn(true);
        when(rideLookup.findParticipants(RIDE_ID)).thenReturn(Optional.of(
                new RideParticipants(RIDE_ID, DRIVER, Set.of(PASSENGER), Set.of(PASSENGER), Set.of())));

        assertFails(ErrorCode.BAD_REQUEST, nonPaymentBy(DRIVER, PASSENGER));

        verifyNoInteractions(paymentDebt);
    }

    @Test
    @DisplayName("Denúncia aceita continua salva mesmo quando a viagem não registra outra dívida")
    void shouldKeepReportWhenNoNewDebtIsRegistered() {
        when(userLookup.exists(PASSENGER)).thenReturn(true);
        rideWithChargedPassenger();
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentDebt.registerNonPayment(RIDE_ID, PASSENGER)).thenReturn(false);

        Report report = useCase.execute(nonPaymentBy(DRIVER, PASSENGER));

        assertThat(report.getReason()).isEqualTo(ReportReason.NON_PAYMENT);
        verify(paymentDebt, times(1)).registerNonPayment(RIDE_ID, PASSENGER);
    }

    private CreateReportUseCase.Command nonPaymentBy(String reporter, String reported) {
        return new CreateReportUseCase.Command(reporter, request(reported, ReportReason.NON_PAYMENT, null, RIDE_ID));
    }

    @Test
    @DisplayName("Falta de pagamento do motorista contra passageiro cobrado deve ser salva e registrar a dívida uma vez")
    void shouldCreateNonPaymentReportAndRegisterDebt() {
        when(userLookup.exists(PASSENGER)).thenReturn(true);
        rideWithChargedPassenger();
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Report report = useCase.execute(nonPaymentBy(DRIVER, PASSENGER));

        assertThat(report.getReason()).isEqualTo(ReportReason.NON_PAYMENT);
        verify(paymentDebt, times(1)).registerNonPayment(RIDE_ID, PASSENGER);
    }

    @Test
    @DisplayName("Só o motorista da viagem pode denunciar falta de pagamento")
    void shouldRejectNonPaymentWhenReporterIsNotTheDriver() {
        when(userLookup.exists(PASSENGER)).thenReturn(true);
        when(rideLookup.findParticipants(RIDE_ID)).thenReturn(Optional.of(
                new RideParticipants(RIDE_ID, DRIVER, Set.of(PASSENGER, STRANGER),
                        Set.of(PASSENGER, STRANGER), Set.of(PASSENGER, STRANGER))));

        assertFails(ErrorCode.NOT_ALLOWED, nonPaymentBy(STRANGER, PASSENGER));

        verifyNoInteractions(paymentDebt);
    }

    @Test
    @DisplayName("Falta de pagamento contra passageiro que não foi cobrado deve ser recusada")
    void shouldRejectNonPaymentWhenPassengerWasNotCharged() {
        when(userLookup.exists(PASSENGER)).thenReturn(true);
        rideWithDriverAndPassenger();

        assertFails(ErrorCode.NOT_ALLOWED, nonPaymentBy(DRIVER, PASSENGER));

        verifyNoInteractions(paymentDebt);
    }

    @Test
    @DisplayName("Segunda denúncia de falta de pagamento do mesmo passageiro na viagem deve ser recusada")
    void shouldRejectSecondNonPaymentForSamePassengerAndRide() {
        when(userLookup.exists(PASSENGER)).thenReturn(true);
        rideWithChargedPassenger();
        when(reportRepository.existsByReportedIdAndRideIdAndReason(PASSENGER, RIDE_ID, ReportReason.NON_PAYMENT))
                .thenReturn(true);

        assertFails(ErrorCode.BAD_REQUEST, nonPaymentBy(DRIVER, PASSENGER));

        verifyNoInteractions(paymentDebt);
    }

    @Test
    @DisplayName("Se a dívida não puder ser registrada, a denúncia continua salva")
    void shouldKeepReportWhenDebtRegistrationFails() {
        when(userLookup.exists(PASSENGER)).thenReturn(true);
        rideWithChargedPassenger();
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentDebt.registerNonPayment(RIDE_ID, PASSENGER)).thenThrow(new IllegalStateException("falhou"));

        Report report = useCase.execute(nonPaymentBy(DRIVER, PASSENGER));

        assertThat(report.getReason()).isEqualTo(ReportReason.NON_PAYMENT);
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    @DisplayName("Outros motivos nunca registram dívida")
    void shouldNotRegisterDebtForOtherReasons() {
        when(userLookup.exists(PASSENGER)).thenReturn(true);
        rideWithChargedPassenger();
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(new CreateReportUseCase.Command(
                DRIVER, request(PASSENGER, ReportReason.LATE_NO_SHOW, null, RIDE_ID)));
        useCase.execute(new CreateReportUseCase.Command(
                DRIVER, request(PASSENGER, ReportReason.INAPPROPRIATE_CONDUCT, null, RIDE_ID)));

        verifyNoInteractions(paymentDebt);
    }

    @Test
    @DisplayName("Denúncia idêntica ainda pendente não deve ser repetida")
    void shouldRejectDuplicatedPendingReport() {
        when(userLookup.exists(DRIVER)).thenReturn(true);
        when(reportRepository.existsByReporterIdAndReportedIdAndRideIdAndReasonAndStatus(
                PASSENGER, DRIVER, null, ReportReason.IMPROPER_CHARGE, ReportStatus.PENDING)).thenReturn(true);

        assertFails(ErrorCode.BAD_REQUEST, new CreateReportUseCase.Command(
                PASSENGER, request(DRIVER, ReportReason.IMPROPER_CHARGE, null, null)));
    }
}
