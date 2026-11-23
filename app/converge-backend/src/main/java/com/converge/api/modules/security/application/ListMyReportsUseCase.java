// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application;

import com.converge.api.modules.security.domain.model.Report;
import com.converge.api.modules.security.domain.repository.ReportRepository;
import com.converge.api.shared.application.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// Lista as denúncias feitas pelo usuário logado, da mais recente para a mais antiga.
@Service
@RequiredArgsConstructor
public class ListMyReportsUseCase implements UseCase<String, List<Report>> {

    private final ReportRepository reportRepository;

    @Override
    public List<Report> execute(String reporterId) {
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId);
    }
}
