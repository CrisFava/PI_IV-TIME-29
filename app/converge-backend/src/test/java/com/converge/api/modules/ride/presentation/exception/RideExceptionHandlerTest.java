package com.converge.api.modules.ride.presentation.exception;

import com.converge.api.modules.ride.application.AcceptPassengerUseCase;
import com.converge.api.modules.ride.application.CancelRideUseCase;
import com.converge.api.modules.ride.application.CompleteRideUseCase;
import com.converge.api.modules.ride.application.GetRideUseCase;
import com.converge.api.modules.ride.application.LeaveRideUseCase;
import com.converge.api.modules.ride.application.ListMyRidesUseCase;
import com.converge.api.modules.ride.application.MarkNoShowUseCase;
import com.converge.api.modules.ride.application.RejectPassengerUseCase;
import com.converge.api.modules.ride.application.RequestRideSeatUseCase;
import com.converge.api.modules.ride.application.StartRideUseCase;
import com.converge.api.modules.ride.application.RideViewService;
import com.converge.api.modules.ride.presentation.controller.RideController;
import com.converge.api.shared.infrastructure.config.auth.AuthenticatedUser;
import com.converge.api.shared.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RideExceptionHandlerTest {

    @Mock private RequestRideSeatUseCase requestRideSeat;
    @Mock private StartRideUseCase startRide;
    @Mock private CompleteRideUseCase completeRide;
    @Mock private MarkNoShowUseCase markNoShow;
    @Mock private LeaveRideUseCase leaveRide;
    @Mock private AcceptPassengerUseCase acceptPassenger;
    @Mock private RejectPassengerUseCase rejectPassenger;
    @Mock private ListMyRidesUseCase listMyRides;
    @Mock private GetRideUseCase getRide;
    @Mock private CancelRideUseCase cancelRide;
    @Mock private RideViewService rideView;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RideController controller = new RideController(
                requestRideSeat, startRide, completeRide, markNoShow, leaveRide,
                acceptPassenger, rejectPassenger, listMyRides, getRide, cancelRide, rideView);

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
                .setControllerAdvice(new GlobalExceptionHandler(), new RideExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUser)
                .build();
    }

    @Test
    @DisplayName("Chave duplicada na Ride deve responder com a mensagem da viagem, e não a de usuário")
    void shouldAnswerRideMessageOnDuplicateKey() throws Exception {
        when(startRide.execute(any())).thenThrow(new DuplicateKeyException("E11000 duplicate key"));

        mockMvc.perform(post("/api/v1/rides/abc/start"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RIDE_CONFLICT"))
                .andExpect(jsonPath("$.error.message", containsString("Tente novamente")));
    }

    @Test
    @DisplayName("Alteração simultânea da mesma viagem deve pedir para tentar de novo")
    void shouldAskToTryAgainOnConcurrentUpdate() throws Exception {
        when(startRide.execute(any())).thenThrow(new OptimisticLockingFailureException("versão antiga"));

        mockMvc.perform(post("/api/v1/rides/abc/start"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RIDE_CONFLICT"))
                .andExpect(jsonPath("$.error.message", containsString("alterada por outra pessoa")));
    }
}
