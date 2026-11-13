// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.indicator.presentation.controller;

import com.converge.api.modules.indicator.application.GetPlatformIndicatorsUseCase;
import com.converge.api.modules.indicator.application.GetUserIndicatorsUseCase;
import com.converge.api.modules.indicator.application.dto.PlatformIndicatorResponseDTO;
import com.converge.api.modules.indicator.application.dto.UserIndicatorResponseDTO;
import com.converge.api.modules.indicator.domain.enums.IndicatorPeriod;
import com.converge.api.modules.user.domain.model.user.User;
import com.converge.api.shared.application.UserUtils;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.infrastructure.exception.GlobalExceptionHandler;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IndicatorControllerTest {

    @Mock
    private GetPlatformIndicatorsUseCase getPlatformIndicators;

    @Mock
    private GetUserIndicatorsUseCase getUserIndicators;

    @Mock
    private UserUtils userUtils;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        IndicatorController controller = new IndicatorController(getPlatformIndicators, getUserIndicators, userUtils);
        // Simula o usuário logado, como o filtro do Firebase faria.
        HandlerMethodArgumentResolver authenticatedUser = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return AuthenticatedUser.class.equals(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {
                return new AuthenticatedUser("firebase-uid", "ana@puc-campinas.edu.br");
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUser)
                .build();
    }

    private static PlatformIndicatorResponseDTO platformDto(IndicatorPeriod period, String reference) {
        return new PlatformIndicatorResponseDTO(period, reference, 10, 120.0, 15, 66.0, 3.0, 7, Instant.now());
    }

    @Test
    @DisplayName("GET /platform sem parâmetros deve usar o período MONTHLY e responder no envelope de sucesso")
    void shouldUseMonthlyByDefaultForPlatform() throws Exception {
        when(getPlatformIndicators.execute(new GetPlatformIndicatorsUseCase.Query(IndicatorPeriod.MONTHLY, null)))
                .thenReturn(platformDto(IndicatorPeriod.MONTHLY, "2026-11"));

        mockMvc.perform(get("/api/v1/indicators/platform"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.period").value("MONTHLY"))
                .andExpect(jsonPath("$.data.reference").value("2026-11"))
                .andExpect(jsonPath("$.data.totalRides").value(10))
                .andExpect(jsonPath("$.data.totalCo2AvoidedKg").value(66.0))
                .andExpect(jsonPath("$.data.equivalentTrees").value(3.0))
                .andExpect(jsonPath("$.data.activeUsers").value(7));
    }

    @Test
    @DisplayName("GET /platform deve repassar o período e a referência informados")
    void shouldForwardPeriodAndReferenceForPlatform() throws Exception {
        GetPlatformIndicatorsUseCase.Query query =
                new GetPlatformIndicatorsUseCase.Query(IndicatorPeriod.WEEKLY, "2026-W46");
        when(getPlatformIndicators.execute(query)).thenReturn(platformDto(IndicatorPeriod.WEEKLY, "2026-W46"));

        mockMvc.perform(get("/api/v1/indicators/platform").param("period", "WEEKLY").param("reference", "2026-W46"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reference").value("2026-W46"));

        verify(getPlatformIndicators).execute(query);
    }

    @Test
    @DisplayName("GET /platform com período inexistente deve responder 400 sem chamar o caso de uso")
    void shouldRejectUnknownPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/indicators/platform").param("period", "DIARIO"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getPlatformIndicators);
    }

    @Test
    @DisplayName("Erro de regra no caso de uso deve voltar no envelope de erro com o código BAD_REQUEST")
    void shouldAnswerApplicationErrorInEnvelope() throws Exception {
        when(getPlatformIndicators.execute(any()))
                .thenThrow(new ApplicationException(ErrorCode.BAD_REQUEST, "Period is required.", Set.of("period")));

        mockMvc.perform(get("/api/v1/indicators/platform"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.fields[0]").value("period"));
    }

    @Test
    @DisplayName("GET /me deve usar o id do usuário logado e o período MONTHLY por padrão")
    void shouldUseLoggedUserForMine() throws Exception {
        ObjectId userId = new ObjectId();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(userUtils.getCurrentUser(any(AuthenticatedUser.class))).thenReturn(user);
        GetUserIndicatorsUseCase.Query query =
                new GetUserIndicatorsUseCase.Query(userId.toHexString(), IndicatorPeriod.MONTHLY, null);
        when(getUserIndicators.execute(query)).thenReturn(new UserIndicatorResponseDTO(
                userId.toHexString(), IndicatorPeriod.MONTHLY, "2026-11", 1, 2, 3, 30.0, 2, 22.0, 1.0, Instant.now()));

        mockMvc.perform(get("/api/v1/indicators/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId.toHexString()))
                .andExpect(jsonPath("$.data.totalRides").value(3))
                .andExpect(jsonPath("$.data.equivalentTrees").value(1.0));

        verify(getUserIndicators).execute(query);
    }

    @Test
    @DisplayName("GET /methodology deve explicar o cálculo com os fatores de emissão usados (RF61)")
    void shouldReturnMethodology() throws Exception {
        mockMvc.perform(get("/api/v1/indicators/methodology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.aviso").exists())
                .andExpect(jsonPath("$.data.carroEvitado").exists())
                .andExpect(jsonPath("$.data.co2").exists())
                .andExpect(jsonPath("$.data.arvoreEquivalente", containsString("22 kg")))
                .andExpect(jsonPath("$.data.fonteEletricidade", containsString("0,0385")))
                .andExpect(jsonPath("$.data.fonteCombustiveis", containsString("2,31")))
                .andExpect(jsonPath("$.data.fonteCombustiveis", containsString("1,50")))
                .andExpect(jsonPath("$.data.fonteCombustiveis", containsString("2,68")));
    }
}
