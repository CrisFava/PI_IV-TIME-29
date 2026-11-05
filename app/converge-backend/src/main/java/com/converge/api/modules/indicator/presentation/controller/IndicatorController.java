package com.converge.api.modules.indicator.presentation.controller;

import com.converge.api.modules.indicator.application.GetPlatformIndicatorsUseCase;
import com.converge.api.modules.indicator.application.dto.PlatformIndicatorResponseDTO;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.shared.presentation.dto.ApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    private final GetPlatformIndicatorsUseCase getPlatformIndicators;

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
