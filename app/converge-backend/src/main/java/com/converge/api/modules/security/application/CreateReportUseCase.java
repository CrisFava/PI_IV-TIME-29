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
import com.converge.api.shared.application.SafeActionHandler;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

// Registra uma denúncia (RF45, RF25). Regras:
//  - não pode denunciar a si mesmo; o denunciado precisa existir;
//  - quando vem uma viagem, reporter e denunciado precisam ter participado dela;
//  - atraso/ausência exige a viagem; "outro" exige descrição (regras do Report);
//  - não repete uma denúncia idêntica que ainda está pendente;
//  - falta de pagamento: só o motorista denuncia, o passageiro precisa ter sido cobrado, uma por passageiro
//    em cada viagem, só depois do "Já paguei", e ela conta como uma dívida (RF67).
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateReportUseCase implements UseCase<CreateReportUseCase.Command, Report> {

    private final ReportRepository reportRepository;
    private final UserLookupGateway userLookup;
    private final RideParticipationLookupGateway rideLookup;
    private final PaymentDebtGateway paymentDebt;

    // Entrada: quem denuncia (do token) + os dados do pedido.
    public record Command(String reporterId, CreateReportRequestDTO request) {
    }

    @Override
    public Report execute(Command command) {
        CreateReportRequestDTO request = command.request();
        Report report = new Report(
                command.reporterId(), request.reportedId(), request.rideId(), request.reason(), request.description());

        if (!userLookup.exists(report.getReportedId())) {
            throw new ApplicationException(ErrorCode.USER_NOT_FOUND, "Usuário não encontrado.");
        }
        if (report.getRideId() != null) {
            RideParticipants ride = assertBothInRide(report);
            if (report.getReason() == ReportReason.NON_PAYMENT) {
                assertValidNonPayment(report, ride);
            }
        }
        if (reportRepository.existsByReporterIdAndReportedIdAndRideIdAndReasonAndStatus(
                report.getReporterId(), report.getReportedId(), report.getRideId(), report.getReason(),
                ReportStatus.PENDING)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Você já fez essa denúncia e ela ainda está em análise.",
                    Set.of("reportedId")
            );
        }
        Report saved = reportRepository.save(report);

        // A denúncia vale mesmo que a dívida não seja registrada agora
        if (saved.getReason() == ReportReason.NON_PAYMENT) {
            SafeActionHandler.run(log, "registrar a dívida da denúncia de falta de pagamento",
                    () -> paymentDebt.registerNonPayment(saved.getRideId(), saved.getReportedId()));
        }
        return saved;
    }

    // Só o motorista denuncia, só passageiro cobrado, e uma denúncia por passageiro em cada viagem
    private void assertValidNonPayment(Report report, RideParticipants ride) {
        if (!report.getReporterId().equals(ride.driverId())) {
            throw new ApplicationException(
                    ErrorCode.NOT_ALLOWED,
                    "Só o motorista da viagem pode denunciar falta de pagamento.",
                    Set.of("reporterId")
            );
        }
        if (!ride.wasCharged(report.getReportedId())) {
            throw new ApplicationException(
                    ErrorCode.NOT_ALLOWED,
                    "Esse passageiro não recebeu cobrança nesta viagem.",
                    Set.of("reportedId")
            );
        }
        if (!ride.canReportNonPayment(report.getReportedId())) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Só dá para denunciar falta de pagamento depois que o passageiro informar o pagamento.",
                    Set.of("rideId")
            );
        }
        if (reportRepository.existsByReportedIdAndRideIdAndReason(
                report.getReportedId(), report.getRideId(), ReportReason.NON_PAYMENT)) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST,
                    "Já existe uma denúncia de falta de pagamento desse passageiro nesta viagem.",
                    Set.of("reportedId")
            );
        }
    }

    private RideParticipants assertBothInRide(Report report) {
        RideParticipants ride = rideLookup.findParticipants(report.getRideId())
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.RIDE_NOT_FOUND, "Viagem não encontrada.", Set.of("rideId")));
        if (!ride.participated(report.getReporterId()) || !ride.participated(report.getReportedId())) {
            throw new ApplicationException(
                    ErrorCode.NOT_ALLOWED,
                    "Só é possível denunciar alguém que participou da mesma viagem que você.",
                    Set.of("rideId")
            );
        }
        return ride;
    }
}
