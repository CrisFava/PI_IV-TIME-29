// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.presentation.controller;

import java.util.List;
import java.time.ZoneId;
import java.time.LocalDate;
import com.converge.api.modules.indicator.application.GetUserIndicatorSeriesUseCase;
import com.converge.api.modules.indicator.application.GetPlatformIndicatorsUseCase;
import com.converge.api.modules.indicator.application.GetUserIndicatorsUseCase;
import com.converge.api.modules.indicator.application.dto.PlatformIndicatorResponseDTO;
import com.converge.api.modules.indicator.application.dto.UserIndicatorResponseDTO;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Autora: Daniela Mikie Kikuchi Gonçalves
// Endpoints dos indicadores de sustentabilidade (RF51-RF61). Só leitura.
@RestController
@RequestMapping("/api/v1/indicators")
@RequiredArgsConstructor
public class IndicatorController {

    // Fuso oficial da aplicação: é nele que "hoje" define a semana e o mês atuais
    private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");

    private final GetPlatformIndicatorsUseCase getPlatformIndicators;
    private final GetUserIndicatorsUseCase getUserIndicators;
    private final GetUserIndicatorSeriesUseCase getUserIndicatorSeries;
    private final UserUtils userUtils;

    // Ex.: GET /api/v1/indicators/platform?period=MONTHLY&reference=2025-05
    @GetMapping("/platform")
    public ResponseEntity<ApplicationResponse<PlatformIndicatorResponseDTO>> getPlatform(
            @RequestParam(defaultValue = "MONTHLY") IndicatorPeriod period,
            @RequestParam(required = false) String reference
    ) {
        PlatformIndicatorResponseDTO data = getPlatformIndicators.execute(
                new GetPlatformIndicatorsUseCase.Query(period, reference)
        );
        return ResponseEntity.ok(ApplicationResponse.success(data));
    }

    // Impacto do próprio usuário logado (RF51 "pelo usuário").
    // Ex.: GET /api/v1/indicators/me?period=MONTHLY&reference=2025-05
    @GetMapping("/me")
    public ResponseEntity<ApplicationResponse<UserIndicatorResponseDTO>> getMine(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "MONTHLY") IndicatorPeriod period,
            @RequestParam(required = false) String reference
    ) {
        String userId = userUtils.getCurrentUser(principal).getId().toHexString();
        UserIndicatorResponseDTO data = getUserIndicators.execute(
                new GetUserIndicatorsUseCase.Query(userId, period, reference)
        );
        return ResponseEntity.ok(ApplicationResponse.success(data));
    }

    // Série para os gráficos: os últimos N períodos, do mais antigo ao mais recente.
    // Ex.: GET /api/v1/indicators/me/series?period=WEEKLY&limit=6
    @GetMapping("/me/series")
    public ResponseEntity<ApplicationResponse<List<UserIndicatorResponseDTO>>> getMySeries(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "MONTHLY") IndicatorPeriod period,
            @RequestParam(defaultValue = "6") int limit
    ) {
        String userId = userUtils.getCurrentUser(principal).getId().toHexString();
        List<UserIndicatorResponseDTO> data = getUserIndicatorSeries.execute(
                new GetUserIndicatorSeriesUseCase.Query(userId, period, limit, LocalDate.now(APP_ZONE))
        );
        return ResponseEntity.ok(ApplicationResponse.success(data));
    }

    // Explica como os números são calculados (RF61).
    @GetMapping("/methodology")
    public ResponseEntity<ApplicationResponse<Map<String, Object>>> getMethodology() {
        Map<String, Object> methodology = Map.of(
                "aviso", "Os valores são estimativas, não medições exatas.",
                "arvoreEquivalente", "1 árvore absorve cerca de 22 kg de CO2 por ano.",
                "carroEvitado", "Cada passageiro efetivo representa 1 carro que deixou de rodar.",
                "co2", "CO2 evitado = energia economizada x fator de emissão da fonte do veículo.",
                "fonteEletricidade", "Fator do Sistema Interligado Nacional (MCTI): 0,0385 kg CO2/kWh (2023).",
                "fonteCombustiveis", "Fatores de referência em kg CO2/litro (GHG Protocol): gasolina 2,31; etanol 1,50; diesel 2,68."
        );
        return ResponseEntity.ok(ApplicationResponse.success(methodology));
    }
}
