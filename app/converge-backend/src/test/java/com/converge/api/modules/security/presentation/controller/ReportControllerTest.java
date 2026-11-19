// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.security.presentation.controller;

import com.converge.api.modules.security.application.CreateReportUseCase;
import com.converge.api.modules.security.application.ListMyReportsUseCase;
import com.converge.api.modules.security.application.dto.CreateReportRequestDTO;
import com.converge.api.modules.security.domain.enums.ReportReason;
import com.converge.api.modules.security.domain.model.Report;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private final ObjectId loggedUserId = new ObjectId();

    @Mock
    private CreateReportUseCase createReport;

    @Mock
    private ListMyReportsUseCase listMyReports;

    @Mock
    private UserUtils userUtils;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReportController controller = new ReportController(createReport, listMyReports, userUtils);
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

    private void loggedUserIsKnown() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(loggedUserId);
        when(userUtils.getCurrentUser(any(AuthenticatedUser.class))).thenReturn(user);
    }

    @Test
    @DisplayName("POST /reports deve denunciar em nome do usuário logado e responder 201 com status PENDING")
    void shouldCreateReportForLoggedUser() throws Exception {
        loggedUserIsKnown();
        when(createReport.execute(any())).thenReturn(
                new Report(loggedUserId.toHexString(), "alvo", "ride-1", ReportReason.LATE_NO_SHOW, null));

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedId\":\"alvo\",\"reason\":\"LATE_NO_SHOW\",\"rideId\":\"ride-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportedId").value("alvo"))
                .andExpect(jsonPath("$.data.reason").value("LATE_NO_SHOW"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.rideId").value("ride-1"));
    }

    @Test
    @DisplayName("POST /reports ignora reporterId do corpo: vale o usuário do token")
    void shouldUseLoggedUserAsReporter() throws Exception {
        loggedUserIsKnown();
        when(createReport.execute(any())).thenReturn(
                new Report(loggedUserId.toHexString(), "alvo", null, ReportReason.IMPROPER_CHARGE, null));

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reporterId\":\"outro\",\"reportedId\":\"alvo\",\"reason\":\"IMPROPER_CHARGE\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateReportUseCase.Command> captor = ArgumentCaptor.forClass(CreateReportUseCase.Command.class);
        verify(createReport).execute(captor.capture());
        assertThat(captor.getValue().reporterId()).isEqualTo(loggedUserId.toHexString());
        assertThat(captor.getValue().request()).isEqualTo(
                new CreateReportRequestDTO("alvo", ReportReason.IMPROPER_CHARGE, null, null));
    }

    @Test
    @DisplayName("POST /reports sem motivo deve responder 400 sem chamar o caso de uso")
    void shouldRejectMissingReason() throws Exception {
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedId\":\"alvo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.fields[0]").value("reason"));

        verifyNoInteractions(createReport);
    }

    @Test
    @DisplayName("POST /reports com descrição acima de 500 caracteres deve responder 400")
    void shouldRejectTooLongDescription() throws Exception {
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedId\":\"alvo\",\"reason\":\"OTHER\",\"description\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0]").value("description"));

        verifyNoInteractions(createReport);
    }

    @Test
    @DisplayName("POST /reports com motivo desconhecido deve responder 400")
    void shouldRejectUnknownReason() throws Exception {
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedId\":\"alvo\",\"reason\":\"QUALQUER\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createReport);
    }

    @Test
    @DisplayName("Erro de regra no caso de uso deve voltar no envelope de erro")
    void shouldAnswerApplicationErrorInEnvelope() throws Exception {
        loggedUserIsKnown();
        when(createReport.execute(any())).thenThrow(
                new ApplicationException(ErrorCode.NOT_ALLOWED, "Só é possível denunciar quem participou da viagem."));

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedId\":\"alvo\",\"reason\":\"INAPPROPRIATE_CONDUCT\",\"rideId\":\"ride-1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_ALLOWED"));
    }

    @Test
    @DisplayName("GET /reports/mine deve listar as denúncias do usuário logado")
    void shouldListMyReports() throws Exception {
        loggedUserIsKnown();
        when(listMyReports.execute(loggedUserId.toHexString())).thenReturn(List.of(
                new Report(loggedUserId.toHexString(), "alvo", null, ReportReason.IMPROPER_CHARGE, "Cobrou a mais")));

        mockMvc.perform(get("/api/v1/reports/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].reportedId").value("alvo"))
                .andExpect(jsonPath("$.data[0].description").value("Cobrou a mais"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }
}
