// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.application;

import com.converge.api.modules.security.domain.enums.ReportReason;
import com.converge.api.modules.security.domain.model.Report;
import com.converge.api.modules.security.domain.repository.ReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListMyReportsUseCaseTest {

    private static final String DRIVER = "driver-1";
    private static final String PASSENGER = "passenger-1";

    @Mock
    private ReportRepository reportRepository;

    @Test
    @DisplayName("Deve listar as denúncias do usuário logado")
    void shouldListMyReports() {
        Report report = new Report(PASSENGER, DRIVER, null, ReportReason.IMPROPER_CHARGE, null);
        when(reportRepository.findByReporterIdOrderByCreatedAtDesc(PASSENGER)).thenReturn(List.of(report));

        assertThat(new ListMyReportsUseCase(reportRepository).execute(PASSENGER)).containsExactly(report);
    }
}