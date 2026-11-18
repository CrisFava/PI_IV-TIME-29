// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application;

import com.converge.api.modules.security.application.dto.CreateReportRequestDTO;
import com.converge.api.modules.security.application.gateway.RideParticipationLookupGateway;
import com.converge.api.modules.security.application.gateway.RideParticipationLookupGateway.RideParticipants;
import com.converge.api.modules.security.application.gateway.UserLookupGateway;
import com.converge.api.modules.security.domain.enums.ReportStatus;
import com.converge.api.modules.security.domain.model.Report;
import com.converge.api.modules.security.domain.repository.ReportRepository;
import com.converge.api.shared.application.UseCase;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

// Registra uma denúncia (RF45, RF25). Regras:
//  - não pode denunciar a si mesmo; o denunciado precisa existir;
//  - quando vem uma viagem, reporter e denunciado precisam ter participado dela;
//  - atraso/ausência exige a viagem; "outro" exige descrição (regras do Report);
//  - não repete uma denúncia idêntica que ainda está pendente.
@Service
@RequiredArgsConstructor
public class CreateReportUseCase implements UseCase<CreateReportUseCase.Command, Report> {

    private final ReportRepository reportRepository;
    private final UserLookupGateway userLookup;
    private final RideParticipationLookupGateway rideLookup;

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
            assertBothInRide(report);
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
        return reportRepository.save(report);
    }

    private void assertBothInRide(Report report) {
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
    }
}
