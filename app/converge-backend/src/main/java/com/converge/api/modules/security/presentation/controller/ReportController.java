// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.presentation.controller;

import com.converge.api.modules.security.application.CreateReportUseCase;
import com.converge.api.modules.security.application.ListMyReportsUseCase;
import com.converge.api.modules.security.application.dto.CreateReportRequestDTO;
import com.converge.api.modules.security.application.dto.ReportResponseDTO;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Endpoints de denúncia (RF45, RF25). A análise pela moderação ainda não tem endpoint.
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final CreateReportUseCase createReport;
    private final ListMyReportsUseCase listMyReports;
    private final UserUtils userUtils;

    // Registra uma denúncia. Quem denuncia é o usuário logado.
    @PostMapping
    public ResponseEntity<ApplicationResponse<ReportResponseDTO>> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateReportRequestDTO request
    ) {
        var report = createReport.execute(new CreateReportUseCase.Command(currentUserId(principal), request));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApplicationResponse.success(ReportResponseDTO.from(report)));
    }

    // Lista as denúncias feitas pelo usuário logado, da mais recente para a mais antiga.
    @GetMapping("/mine")
    public ResponseEntity<ApplicationResponse<List<ReportResponseDTO>>> listMine(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        List<ReportResponseDTO> reports = listMyReports.execute(currentUserId(principal)).stream()
                .map(ReportResponseDTO::from)
                .toList();
        return ResponseEntity.ok(ApplicationResponse.success(reports));
    }

    private String currentUserId(AuthenticatedUser principal) {
        return userUtils.getCurrentUser(principal).getId().toHexString();
    }
}
