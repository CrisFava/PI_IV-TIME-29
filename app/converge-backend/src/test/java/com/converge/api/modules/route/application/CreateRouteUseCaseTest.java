package com.converge.api.modules.route.application;

import com.converge.api.modules.route.application.dto.CreateRouteCommand;
import com.converge.api.modules.route.application.dto.CreateRouteRequestDTO;
import com.converge.api.modules.route.application.dto.PlaceRequestDTO;
import com.converge.api.modules.route.application.mapper.RouteMapper;
import com.converge.api.modules.route.domain.enums.RouteType;
import com.converge.api.modules.route.domain.model.Route;
import com.converge.api.modules.route.domain.repository.RouteRepository;
import com.converge.api.shared.presentation.exception.ApplicationException;
import com.converge.api.shared.presentation.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateRouteUseCaseTest {

    @Mock
    private RouteRepository routeRepository;

    private CreateRouteUseCase createRouteUseCase;

    @BeforeEach
    void setUp() {
        createRouteUseCase = new CreateRouteUseCase(routeRepository, new RouteMapper());
    }

    private CreateRouteCommand offerCommand() {
        CreateRouteRequestDTO request = new CreateRouteRequestDTO(
                RouteType.OFFER,
                "vehicle-456",
                new PlaceRequestDTO("Rua Barão de Jaguara, 1000", "Centro", "Campinas", -22.9056, -47.0608),
                new PlaceRequestDTO("Rod. Dom Pedro I, km 136", null, "Campinas", -22.8337, -47.0524),
                "07:00",
                "07:40",
                Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                2.0,
                3
        );
        return new CreateRouteCommand("user-123", request);
    }

    private void noDuplicate() {
        when(routeRepository.existsLiveDuplicate(
                anyString(), any(), anyString(), anyString(), anyString(), anyCollection()
        )).thenReturn(false);
    }

    @Test
    @DisplayName("Deve salvar o trajeto com o dono vindo do command")
    void shouldCreateRoute() {
        noDuplicate();
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Route route = createRouteUseCase.execute(offerCommand());

        assertThat(route.getUserId()).isEqualTo("user-123");
        assertThat(route.getType()).isEqualTo(RouteType.OFFER);
        verify(routeRepository).save(route);
    }

    @Test
    @DisplayName("Deve consultar duplicados com os dados do trajeto")
    void shouldCheckDuplicatesWithRouteData() {
        noDuplicate();
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        createRouteUseCase.execute(offerCommand());

        verify(routeRepository).existsLiveDuplicate(
                eq("user-123"),
                eq(RouteType.OFFER),
                eq("Rua Barão de Jaguara, 1000"),
                eq("Rod. Dom Pedro I, km 136"),
                eq("07:00"),
                anyCollection()
        );
    }

    @Test
    @DisplayName("Deve lançar ROUTE_ALREADY_EXISTS e não salvar quando já houver trajeto igual ativo")
    void shouldRejectDuplicatedRoute() {
        when(routeRepository.existsLiveDuplicate(
                anyString(), any(), anyString(), anyString(), anyString(), anyCollection()
        )).thenReturn(true);

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> createRouteUseCase.execute(offerCommand())
        );

        assertThat(ex.getError().code()).isEqualTo(ErrorCode.ROUTE_ALREADY_EXISTS);
        verify(routeRepository, never()).save(any());
    }
}
